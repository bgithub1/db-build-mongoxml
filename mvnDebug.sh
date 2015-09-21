mvnDebug exec:exec -e -DforkMode=never  -Dexec.executable="java" -Dexec.args="-Xmx1500m -Xms300m -cp %classpath  ${*:1}"

