#!/bin/bash

if [[ "$2" == "meta" ]]
then
  nodefillcolor=slategray1
  nodehighlight=darkseagreen1
  nodehighlight2=aquamarine
  edgehighlight=mediumblue
  boxcolor=black
else
  nodefillcolor=ivory1
  nodehighlight=khaki1
  nodehighlight2=lemonchiffon1
  edgehighlight=mediumblue
  boxcolor=black
fi

nodeshape=box
nodestyle=filled,rounded
#nodeheight=0.37
nodesep=0.4

arrowhead=vee
arrowsize=0.75

graphfont=Sans
nodefont=$graphfont
edgefont=$graphfont

nodefontsize=10
edgefontsize=$nodefontsize

indata=$(cat);
indata=${indata//NODEHIGHLIGHT/$nodehighlight}
indata=${indata//NODE2HIGHLIGHT/$nodehighlight2}
indata=${indata//EDGEHIGHLIGHT/$edgehighlight}
indata=${indata//BOXCOLOR/$boxcolor}

svgfile=$1
pngfile="${svgfile%.svg}.png"

prepend="digraph g{ \
  node [shape=\"$nodeshape\" fillcolor=\"$nodefillcolor\" style=\"$nodestyle\" \
    fontsize=$nodefontsize fontname=\"$nodefont\"]
  edge [arrowhead=\"$arrowhead\" arrowsize=$arrowsize fontsize=$edgefontsize fontname=\"$edgefont\"] \
  nodesep=$nodesep \
  fontname=\"$graphfont\" "

echo "${prepend} ${indata} }" | dot -Tpng -o"$pngfile"
echo "${prepend} ${indata} }" | dot -Tsvg -o"$svgfile"

echo ""

