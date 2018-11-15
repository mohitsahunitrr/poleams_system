#!/bin/bash
feeder=$1
pole=$2
cd "$pole"
for file in rgb_DJI_*ML*
do
	java -jar "$HOME/workspace/poleams_system/poleams-workbench/target/workbench.jar" uploadResource -env 'InspecTools Dev' -type IdentifiedComponents -feeder "$feeder" -replace -fplid "$pole" $file
done
