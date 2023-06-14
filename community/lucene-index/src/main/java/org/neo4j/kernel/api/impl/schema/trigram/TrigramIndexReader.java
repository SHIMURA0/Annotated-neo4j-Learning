/*
 * Copyright (c) "Neo4j"
 * Neo4j Sweden AB [http://neo4j.com]
 *
 * This file is part of Neo4j.
 *
 * Neo4j is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.neo4j.kernel.api.impl.schema.trigram;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Iterator;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TotalHitCountCollector;
import org.neo4j.internal.helpers.collection.BoundedIterable;
import org.neo4j.internal.helpers.collection.PrefetchingIterator;
import org.neo4j.internal.kernel.api.PropertyIndexQuery;
import org.neo4j.internal.kernel.api.PropertyIndexQuery.ExactPredicate;
import org.neo4j.internal.kernel.api.PropertyIndexQuery.StringContainsPredicate;
import org.neo4j.internal.kernel.api.PropertyIndexQuery.StringPrefixPredicate;
import org.neo4j.internal.kernel.api.PropertyIndexQuery.StringSuffixPredicate;
import org.neo4j.internal.schema.IndexDescriptor;
import org.neo4j.io.pagecache.context.CursorContext;
import org.neo4j.kernel.api.impl.index.SearcherReference;
import org.neo4j.kernel.api.impl.index.collector.DocValuesCollector;
import org.neo4j.kernel.api.impl.schema.AbstractTextIndexReader;
import org.neo4j.kernel.api.impl.schema.TaskCoordinator;
import org.neo4j.kernel.api.index.IndexSampler;
import org.neo4j.kernel.impl.api.index.IndexSamplingConfig;
import org.neo4j.kernel.impl.index.schema.IndexUsageTracker;
import org.neo4j.util.Preconditions;
import org.neo4j.values.storable.Value;

public class TrigramIndexReader extends AbstractTextIndexReader {
    TrigramIndexReader(
            SearcherReference searcherReference,
            IndexDescriptor descriptor,
            IndexSamplingConfig samplingConfig,
            TaskCoordinator taskCoordinator,
            IndexUsageTracker usageTracker) {
        super(descriptor, searcherReference, samplingConfig, taskCoordinator, usageTracker);
    }

    @Override
    public IndexSampler createSampler() {
        return new TrigramIndexSampler(getIndexSearcher());
    }

    @Override
    protected Query toLuceneQuery(PropertyIndexQuery predicate) {
        return switch (predicate.type()) {
            case ALL_ENTRIES -> TrigramQueryFactory.allValues();
            case EXACT -> {
                final var value =
                        ((ExactPredicate) predicate).value().asObject().toString();
                yield TrigramQueryFactory.exact(value);
            }
            case STRING_PREFIX -> {
                final var spp = (StringPrefixPredicate) predicate;
                yield TrigramQueryFactory.stringPrefix(spp.prefix().stringValue());
            }
            case STRING_CONTAINS -> {
                final var scp = (StringContainsPredicate) predicate;
                yield TrigramQueryFactory.stringContains(scp.contains().stringValue());
            }
            case STRING_SUFFIX -> {
                final var ssp = (StringSuffixPredicate) predicate;
                yield TrigramQueryFactory.stringSuffix(ssp.suffix().stringValue());
            }
            default -> throw invalidQuery(predicate);
        };
    }

    @Override
    protected String entityIdFieldKey() {
        return TrigramDocumentStructure.ENTITY_ID_KEY;
    }

    @Override
    protected boolean needStoreFilter(PropertyIndexQuery predicate) {
        return TrigramQueryFactory.needStoreFilter(predicate);
    }

    /**
     * This isn't perfect. We will get false positives for searches where additional trigrams
     * than the trigrams of the property value is also stored in the index.
     * But since we are doing a query for the specific entity id it should be a pretty safe bet that if
     * we find an entity with the matching id and all the trigrams of our search word are indexed, then what
     * is stored in the index is most likely indexed correctly.
     * Don't use this for anything critical, but for a best effort consistency check it is fine.
     */
    @Override
    public long countIndexedEntities(
            long entityId, CursorContext cursorContext, int[] propertyKeyIds, Value... propertyValues) {
        final var entityIdQuery = TrigramQueryFactory.getById(entityId);

        final var entityIdAndValueQuery = new BooleanQuery.Builder();
        entityIdAndValueQuery.add(entityIdQuery, BooleanClause.Occur.MUST);

        Preconditions.checkState(
                propertyKeyIds.length == 1,
                "Text index does not support composite indexing. Tried to query index with multiple property keys.");
        final var value = propertyValues[0].asObject().toString();
        final var valueQuery = TrigramQueryFactory.exact(value);
        entityIdAndValueQuery.add(valueQuery, BooleanClause.Occur.MUST);

        try {
            final var collector = new TotalHitCountCollector();
            getIndexSearcher().search(entityIdAndValueQuery.build(), collector);
            // A <label,propertyKeyId,nodeId> tuple should only match at most a single propertyValue
            return collector.getTotalHits();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    BoundedIterable<Long> newAllEntriesValueReader(long fromIdInclusive, long toIdExclusive) throws IOException {
        DocValuesCollector collector = new DocValuesCollector();
        getIndexSearcher().search(TrigramQueryFactory.allValues(), collector);
        var entityConsumer = new DocValuesCollector.EntityConsumer() {

            long reference;

            @Override
            public boolean acceptEntity(long reference, float score, Value... values) {
                if (reference >= fromIdInclusive && reference < toIdExclusive) {
                    this.reference = reference;
                    return true;
                } else {
                    return false;
                }
            }
        };
        var indexProgressor = collector.getIndexProgressor(TrigramDocumentStructure.ENTITY_ID_KEY, entityConsumer);
        return new BoundedIterable<>() {

            @Override
            public Iterator<Long> iterator() {
                return new PrefetchingIterator<>() {

                    @Override
                    protected Long fetchNextOrNull() {
                        if (indexProgressor.next()) {
                            return entityConsumer.reference;
                        }
                        return null;
                    }
                };
            }

            @Override
            public void close() {
                indexProgressor.close();
            }

            @Override
            public long maxCount() {
                return UNKNOWN_MAX_COUNT;
            }
        };
    }
}
