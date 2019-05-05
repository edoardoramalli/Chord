#!/bin/bash

while getopts p:a:b:c:d: option
do
    case "${option}" in
        p) PORT=${OPTARG};;
        a) CIP=${OPTARG};;
        b) CP=${OPTARG};;
        c) JIP=${OPTARG};;
        d) JP=${OPTARG};;
    esac
done

cmd="java -jar ~/Desktop/main.jar -t 2 -p $PORT -cip $CIP -cp $CP -jip $JIP -jp $JP"
echo "Command : $cmd"
osascript -e "tell application \"Terminal\" to do script \"$cmd\""

read -p "Press q to kill Java : " name
if [ "$name" == "q" ]; then
    killall java
    break
fi

