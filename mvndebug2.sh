mvn exec:exec -e  -Dexec.executable="java" -Dexec.args="-Xmx1500m -Xms300m -Xdebug -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=8000  -cp %classpath  ${*:1}"

