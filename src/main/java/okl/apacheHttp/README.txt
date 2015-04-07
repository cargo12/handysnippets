# basic http server using apache http components and http client using
# HttpURLConnection

for i in ~/.m2/repository/org/apache/httpcomponents/httpclient/4.2.5/*jar; do cpa="$cpa:$i";done
java -cp target/classes/:$cpa:~/.m2/repository/org/apache/httpcomponents/httpcore/4.2.4/httpcore-4.2.4.jar okl.apacheHttp.HCoreTest

java -cp target/classes/ okl.apacheHttp.HucTest

echo -en "GET /personal/ HTTP/1.1\r\nConnection: Close\r\n\r\n" |nc localhost 8801
