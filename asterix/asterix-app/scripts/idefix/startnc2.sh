#/bin/bash

export JAVA_OPTS="-DAsterixConfigFileName=test.properties -Djava.util.logging.config.file=/home/nicnic/Work/Asterix/hyracks/logging.properties"
export HYRACKS_HOME="/home/nicnic/workspace/hyracks/tags/hyracks-0.1.5"

bash ${HYRACKS_HOME}/hyracks-server/target/appassembler/bin/hyracksnc -cc-host 127.0.0.1 -data-ip-address 127.0.0.1 -node-id "nc2" $*

