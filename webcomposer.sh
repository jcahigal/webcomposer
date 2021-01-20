#!/bin/bash

if [ $# -eq 1 ] && [ "$1" == "-h" ]
then
  echo "run './webcomposer.sh' to work with screenshots or './webcomposer.sh -f' to work with iframes"
elif [ $# -eq 1 ] && [ "$1" == "-f" ]
then
  rm *.html
  rm *.png
  ./webcomposer.kts
  open tiempo.html
else
  rm *.html
  rm *.png
  ./webscreenshots.kts
  open tiempoS.html
fi

