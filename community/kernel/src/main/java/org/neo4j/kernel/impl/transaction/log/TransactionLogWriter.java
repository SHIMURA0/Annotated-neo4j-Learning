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
package org.neo4j.kernel.impl.transaction.log;

import java.io.IOException;
import java.nio.ByteBuffer;
import org.neo4j.kernel.impl.transaction.CommittedCommandBatch;
import org.neo4j.kernel.impl.transaction.log.entry.LogEntryWriter;
import org.neo4j.storageengine.api.CommandBatch;
import org.neo4j.util.VisibleForTesting;

public class TransactionLogWriter {
    private final FlushablePositionAwareChecksumChannel channel;
    private final LogEntryWriter<FlushablePositionAwareChecksumChannel> writer;

    public TransactionLogWriter(FlushablePositionAwareChecksumChannel channel) {
        this(channel, new LogEntryWriter<>(channel));
    }

    @VisibleForTesting
    public TransactionLogWriter(
            FlushablePositionAwareChecksumChannel channel,
            LogEntryWriter<FlushablePositionAwareChecksumChannel> writer) {
        this.channel = channel;
        this.writer = writer;
    }

    /*
    * It's possible to append two types of transactions: complete or chunked.
    * Complete transactions are transactions that come with all desired changes in one command batch.
    * This kind of transaction is stored in logs as a sequence of commands like:
    *   start entry -> transactional content -> commit entry
    * Chunked transactions are stored in logs in different chunks where each chunk contains only part of the transaction.
    * This kind of transaction is stored in logs as a sequence of commands like:
        start entry -> chunk start > chunk content -> chunk end -> chunk start > chunk content -> chunk end -> commit entry
    * </p>
    * An important difference is chunk start/chunk end command pairs. They present only in chunked transactions.
    * The first chunk of chunked transactions comes also with a start entry.
    * The last chunk in the chunked transaction comes with a commit entry at the end.
    */
    public int append(CommandBatch batch, long transactionId, long chunkId, int previousChecksum) throws IOException {
        byte version = batch.kernelVersion().version();

        if (batch.isFirst()) {
            writer.writeStartEntry(
                    version,
                    batch.getTimeStarted(),
                    batch.getLatestCommittedTxWhenStarted(),
                    previousChecksum,
                    batch.additionalHeader());
        } else {
            writer.writeChunkStartEntry(version, batch.getTimeCommitted(), chunkId);
        }

        // Write all the commands to the log channel
        writer.serialize(batch);

        if (batch.isLast()) {
            return writer.writeCommitEntry(version, transactionId, batch.getTimeCommitted());
        } else {
            return writer.writeChunkEndEntry(version, transactionId, chunkId);
        }
    }

    public int append(CommittedCommandBatch commandBatch) throws IOException {
        return commandBatch.serialize(writer);
    }

    public LogPosition getCurrentPosition() throws IOException {
        return channel.getCurrentPosition();
    }

    public LogPositionMarker getCurrentPosition(LogPositionMarker logPositionMarker) throws IOException {
        return channel.getCurrentPosition(logPositionMarker);
    }

    @VisibleForTesting
    public FlushablePositionAwareChecksumChannel getChannel() {
        return channel;
    }

    public void append(ByteBuffer byteBuffer) throws IOException {
        channel.write(byteBuffer);
    }

    @VisibleForTesting
    public LogEntryWriter<FlushablePositionAwareChecksumChannel> getWriter() {
        return writer;
    }
}
