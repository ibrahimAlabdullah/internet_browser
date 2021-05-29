package Connction;




import nodes.Document;
import nodes.Element;
import parserr.Parser;
import parserr.TokenQueue;

import java.io.*;
import java.nio.ByteBuffer;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLSocketFactory;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.*;
import java.nio.charset.IllegalCharsetNameException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import java.lang.*;
import static org.jsoup.helper.DataUtil.readToByteBuffer;


/**
 * Implementation of {@link }.
 * @see org.jsoup.Jsoup#connect(String)
 */

public class HttpConnection {
    public static final String CONTENT_ENCODING = "Content-Encoding";
    static final String defaultCharset = "UTF-8";
    private static final int firstReadBufferSize = 1024 * 5;
    static final int bufferSize = 1024 * 32;
    private static final char[] mimeBoundaryChars =
            "-_1234567890abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    static final int boundaryLength = 32;
    /**
     * Many users would get caught by not setting a user-agent and therefore getting different responses on their desktop
     * vs in jsoup, which would otherwise default to {@code Java}. So by default, use a desktop UA.
     */
    public static final String DEFAULT_UA =
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_11_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/53.0.2785.143 Safari/537.36";
    private static final String USER_AGENT = "User-Agent";
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String MULTIPART_FORM_DATA = "multipart/form-data";
    public static final String FORM_URL_ENCODED = "application/x-www-form-urlencoded";
    private static final int HTTP_TEMP_REDIR = 307; // http/1.1 temporary redirect, not in Java's set.
    private static final String DefaultUploadType = "application/octet-stream";
   public static  enum Method {
        GET(false), POST(true), PUT(true), DELETE(false), PATCH(true), HEAD(false), OPTIONS(false), TRACE(false);

        private final boolean hasBody;

        Method(boolean hasBody) {
            this.hasBody = hasBody;
        }

        /**
         * Check if this HTTP method has/needs a request body
         * @return if body needed
         */
        public final boolean hasBody() {
            return hasBody;
        }
    }
    public static void notNull(Object obj, String msg) {
        if (obj == null)
            throw new IllegalArgumentException(msg);
    }
    public static void notEmpty(String string) {
        if (string == null || string.length() == 0)
            throw new IllegalArgumentException("String must not be empty");
    }
    public static void isFalse(boolean val, String msg) {
        if (val)
            throw new IllegalArgumentException(msg);
    }
    public static void isTrue(boolean val, String msg) {
        if (!val)
            throw new IllegalArgumentException(msg);
    }
    public static void notNull(Object obj) {
        if (obj == null)
            throw new IllegalArgumentException("Object must not be null");
    }

    public static void notEmpty(String string, String msg) {
        if (string == null || string.length() == 0)
            throw new IllegalArgumentException(msg);
    }

    public static HttpConnection connect(String url) {
        HttpConnection con = new HttpConnection();
        con.url(url);
        return con;
    }

    public static HttpConnection connect(URL url) {
        HttpConnection con = new HttpConnection();
        con.url(url);
        return con;
    }

    public HttpConnection() {
        req = new HttpConnection.Request();
        res = new Response();
    }
    private static String validateCharset(String cs) {
        if (cs == null || cs.length() == 0) return null;
        cs = cs.trim().replaceAll("[\"']", "");
        try {
            if (Charset.isSupported(cs)) return cs;
            cs = cs.toUpperCase(Locale.ENGLISH);
            if (Charset.isSupported(cs)) return cs;
        } catch (IllegalCharsetNameException e) {
            // if our this charset matching fails.... we just take the default
        }
        return null;
    }
    private static BomCharset detectCharsetFromBom(final ByteBuffer byteData) {
        final Buffer buffer = byteData; // .mark and rewind used to return Buffer, now ByteBuffer, so cast for backward compat
        buffer.mark();
        byte[] bom = new byte[4];
        if (byteData.remaining() >= bom.length) {
            byteData.get(bom);
            buffer.rewind();
        }
        if (bom[0] == 0x00 && bom[1] == 0x00 && bom[2] == (byte) 0xFE && bom[3] == (byte) 0xFF || // BE
                bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE && bom[2] == 0x00 && bom[3] == 0x00) { // LE
            return new BomCharset("UTF-32", false); // and I hope it's on your system
        } else if (bom[0] == (byte) 0xFE && bom[1] == (byte) 0xFF || // BE
                bom[0] == (byte) 0xFF && bom[1] == (byte) 0xFE) {
            return new BomCharset("UTF-16", false); // in all Javas
        } else if (bom[0] == (byte) 0xEF && bom[1] == (byte) 0xBB && bom[2] == (byte) 0xBF) {
            return new BomCharset("UTF-8", true); // in all Javas
            // 16 and 32 decoders consume the BOM to determine be/le; utf-8 should be consumed here
        }
        return null;
    }
  public  static Document parseInputStream(InputStream input, String charsetName, String baseUri, Parser parser) throws IOException  {
        if (input == null) // empty body
            return new Document(baseUri);
        input = ConstrainableInputStream.wrap(input, bufferSize, 0);

        Document doc = null;
        boolean fullyRead = false;

        // read the start of the stream and look for a BOM or meta charset
        input.mark(bufferSize);
        ByteBuffer firstBytes = readToByteBuffer(input, firstReadBufferSize - 1); // -1 because we read one more to see if completed. First read is < buffer size, so can't be invalid.
        fullyRead = input.read() == -1;
        input.reset();

        // look for BOM - overrides any other header or input
        BomCharset bomCharset = detectCharsetFromBom(firstBytes);
        if (bomCharset != null)
            charsetName = bomCharset.charset;

        if (charsetName == null) { // determine from meta. safe first parse as UTF-8
            String docData = Charset.forName(defaultCharset).decode(firstBytes).toString();
            doc = parser.parseInput(docData, baseUri);

            // look for <meta http-equiv="Content-Type" content="text/html;charset=gb2312"> or HTML5 <meta charset="gb2312">
            Elements metaElements = doc.select("meta[http-equiv=content-type], meta[charset]");
            String foundCharset = null; // if not found, will keep utf-8 as best attempt
            for (Element meta : metaElements) {
                if (meta.hasAttr("http-equiv"))
                    foundCharset =defaultCharset;                if (foundCharset == null && meta.hasAttr("charset"))
                    foundCharset = meta.attr("charset");
                if (foundCharset != null)
                    break;
            }

            // look for <?xml encoding='ISO-8859-1'?>

            foundCharset = validateCharset(foundCharset);
            if (foundCharset != null && !foundCharset.equalsIgnoreCase(defaultCharset)) { // need to re-decode. (case insensitive check here to match how validate works)
                foundCharset = foundCharset.trim().replaceAll("[\"']", "");
                charsetName = foundCharset;
                doc = null;
            } else if (!fullyRead) {
                doc = null;
            }
        } else { // specified by content type header (or by user on file load)

            if (charsetName== null || charsetName.length() == 0)
                throw new IllegalArgumentException("Must set charset arg to character set of file to parse. Set to null to attempt to detect from HTML");

        }
        if (doc == null) {
            if (charsetName == null)
                charsetName = defaultCharset;
            BufferedReader reader = new BufferedReader(new InputStreamReader(input, charsetName), bufferSize);
            if (bomCharset != null && bomCharset.offset) // creating the buffered reader ignores the input pos, so must skip here
                reader.skip(1);
            try {
                doc = parser.parseInput(reader, baseUri);
            } catch (RuntimeException e) {
                // io exception when parsing (not seen before because reading the stream as we go)

            }
            Charset charset = Charset.forName(charsetName);
            doc.outputSettings().charset(charset);
            if (!charset.canEncode()) {
                // some charsets can read but not encode; switch to an encodable charset and update the meta el
                doc.charset(Charset.forName(defaultCharset));
            }
        }
        input.close();
        return doc;
    }
    /**
     * Encodes the input URL into a safe ASCII URL string
     * @param url unescaped URL
     * @return escaped URL
     */
	private static String encodeUrl(String url) {
        try {
            URL u = new URL(url);
            return encodeUrl(u).toExternalForm();
        } catch (Exception e) {
            return url;
        }
	}

    static URL encodeUrl(URL u) {
        try {
            //  odd way to encode urls, but it works!
            String urlS = u.toExternalForm(); // URL external form may have spaces which is illegal in new URL() (odd asymmetry)
            urlS = urlS.replaceAll(" ", "%20");
            final URI uri = new URI(urlS);
            return new URL(uri.toASCIIString());
        } catch (Exception e) {
            return u;
        }
    }

    private static String encodeMimeName(String val) {
        if (val == null)
            return null;
        return val.replaceAll("\"", "%22");
    }

    private HttpConnection.Request req;
    private HttpConnection.Response res;

    public HttpConnection url(URL url) {
        req.url(url);
        return this;
    }

    public HttpConnection url(String url) {
      /* Validate.notEmpty(url, "Must supply a valid URL");*/ 
        try {
            req.url(new URL(encodeUrl(url)));
        } catch (MalformedURLException e) {
            throw new IllegalArgumentException("Malformed URL: " + url, e);
        }
        return this;
    }



    static ByteBuffer emptyByteBuffer() {
        return ByteBuffer.allocate(0);
    }
    public HttpConnection userAgent(String userAgent) {
        notNull(userAgent, "User agent must not be null");
        req.header(USER_AGENT, userAgent);
        return this;
    }

    public HttpConnection timeout(int millis) {
        req.timeout(millis);
        return this;
    }

    public HttpConnection maxBodySize(int bytes) {
        req.maxBodySize(bytes);
        return this;
    }

    public HttpConnection followRedirects(boolean followRedirects) {
        req.followRedirects(followRedirects);
        return this;
    }

    public HttpConnection referrer(String referrer) {
        notNull(referrer, "Referrer must not be null");
        req.header("Referer", referrer);
        return this;
    }

    public HttpConnection method(Method method) {
        req.method(method);
        return this;
    }

    public HttpConnection ignoreHttpErrors(boolean ignoreHttpErrors) {
		req.ignoreHttpErrors(ignoreHttpErrors);
		return this;
	}

    public HttpConnection ignoreContentType(boolean ignoreContentType) {
        req.ignoreContentType(ignoreContentType);
        return this;
    }


    public HttpConnection data(String key, String value) {
        req.data(KeyVal.create(key, value));
        return this;
    }

    public HttpConnection sslSocketFactory(SSLSocketFactory sslSocketFactory) {
	    req.sslSocketFactory(sslSocketFactory);
	    return this;
    }

    public HttpConnection data(String key, String filename, InputStream inputStream) {
        req.data(KeyVal.create(key, filename, inputStream));
        return this;
    }
    static void crossStreams(final InputStream in, final OutputStream out) throws IOException {
        final byte[] buffer = new byte[bufferSize];
        int len;
        while ((len = in.read(buffer)) != -1) {
            out.write(buffer, 0, len);
        }
    }

    public HttpConnection data(String key, String filename, InputStream inputStream, String contentType) {
        req.data(KeyVal.create(key, filename, inputStream).contentType(contentType));
        return this;
    }

    public HttpConnection data(Map<String, String> data) {
      //  Validate.notNull(data, "Data map must not be null");
        for (Map.Entry<String, String> entry : data.entrySet()) {
            req.data(KeyVal.create(entry.getKey(), entry.getValue()));
        }
        return this;
    }

    public HttpConnection data(String... keyvals) {
       /* Validate.notNull(keyvals, "Data key value pairs must not be null");
        Validate.isTrue(keyvals.length %2 == 0, "Must supply an even number of key value pairs");
        */
    	for (int i = 0; i < keyvals.length; i += 2) {
            String key = keyvals[i];
            String value = keyvals[i+1];
           /* Validate.notEmpty(key, "Data key must not be empty");
            Validate.notNull(value, "Data value must not be null");*/
            req.data(KeyVal.create(key, value));
        }
        return this;
    }

    public HttpConnection data(Collection<HttpConnection.KeyVal> data) {
        /*Validate.notNull(data, "Data collection must not be null");*/
        for (HttpConnection.KeyVal entry: data) {
            req.data(entry);
        }
        return this;
    }

    public HttpConnection.KeyVal data(String key) {
        /*Validate.notEmpty(key, "Data key must not be empty");*/
        for (HttpConnection.KeyVal keyVal : request().data()) {
            if (keyVal.key().equals(key))
                return keyVal;
        }
        return null;
    }

    public HttpConnection requestBody(String body) {
        req.requestBody(body);
        return this;
    }

    public HttpConnection header(String name, String value) {
        req.header(name, value);
        return this;
    }

    public HttpConnection headers(Map<String,String> headers) {
       /* Validate.notNull(headers, "Header map must not be null");*/
        for (Map.Entry<String,String> entry : headers.entrySet()) {
            req.header(entry.getKey(),entry.getValue());
        }
        return this;
    }

 

   

    public HttpConnection parser(Parser parser) {
        req.parser(parser);
        return this;
    }

    public Document get() throws IOException {
        req.method(Method.GET);
        execute();
        return res.parse();
    }

    public Document post() throws IOException {
        req.method(Method.POST);
        execute();
        return res.parse();
    }

    public HttpConnection.Response execute() throws IOException {
        res = Response.execute(req);
        return res;
    }

    public HttpConnection.Request request() {
        return req;
    }

    public HttpConnection request(HttpConnection.Request request) {
        req = request;
        return this;
    }

    public HttpConnection.Response response() {
        return res;
    }

    public HttpConnection response(HttpConnection.Response response) {
        res = response;
        return this;
    }

    public HttpConnection postDataCharset(String charset) {
        req.postDataCharset(charset);
        return this;
    }

    @SuppressWarnings({"unchecked"})
    private static abstract class Base<T extends HttpConnection.Base<?>>  {
        URL url;
        Method method;
        Map<String, List<String>> headers;
        Map<String, String> cookies;

        private Base() {
            headers = new LinkedHashMap<>();
            cookies = new LinkedHashMap<>();
        }

        public URL url() {
            return url;
        }

        public T url(URL url) {
           /* Validate.notNull(url, "URL must not be null"); */
            this.url = url;
            return (T) this;
        }

        public Method method() {
            return method;
        }

        public T method(Method method) {
            /*Validate.notNull(method, "Method must not be null");*/
            this.method = method;
            return (T) this;
        }

        public String header(String name) {
            /*Validate.notNull(name, "Header name must not be null");*/
            List<String> vals = getHeadersCaseInsensitive(name);
            if (vals.size() > 0) {
                // https://www.w3.org/Protocols/rfc2616/rfc2616-sec4.html#sec4.2
                return StringUtil.join(vals, ", ");
            }

            return null;
        }


        public T addHeader(String name, String value) {
           /* Validate.notEmpty(name);*/
            value = value == null ? "" : value;

            List<String> values = headers(name);
            if (values.isEmpty()) {
                values = new ArrayList<>();
                headers.put(name, values);
            }
            values.add(fixHeaderEncoding(value));

            return (T) this;
        }


        public List<String> headers(String name) {
            
            return getHeadersCaseInsensitive(name);
        }

        private static String fixHeaderEncoding(String val) {
            try {
                byte[] bytes = val.getBytes("ISO-8859-1");
                if (!looksLikeUtf8(bytes))
                    return val;
                return new String(bytes, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // shouldn't happen as these both always exist
                return val;
            }
        }

        private static boolean looksLikeUtf8(byte[] input) {
            int i = 0;
            // BOM:
            if (input.length >= 3 && (input[0] & 0xFF) == 0xEF
                && (input[1] & 0xFF) == 0xBB & (input[2] & 0xFF) == 0xBF) {
                i = 3;
            }

            int end;
            for (int j = input.length; i < j; ++i) {
                int o = input[i];
                if ((o & 0x80) == 0) {
                    continue; // ASCII
                }

                // UTF-8 leading:
                if ((o & 0xE0) == 0xC0) {
                    end = i + 1;
                } else if ((o & 0xF0) == 0xE0) {
                    end = i + 2;
                } else if ((o & 0xF8) == 0xF0) {
                    end = i + 3;
                } else {
                    return false;
                }

                while (i < end) {
                    i++;
                    o = input[i];
                    if ((o & 0xC0) != 0x80) {
                        return false;
                    }
                }
            }
            return true;
        }

        public T header(String name, String value) {
   if(name==null)System.out.println( "Header name must not be empty");
            removeHeader(name); // ensures we don't get an "accept-encoding" and a "Accept-Encoding"
            addHeader(name, value);
            return (T) this;
        }

        public boolean hasHeader(String name) {
        	if(name==null)System.out.println( "Header name must not be empty");
            return getHeadersCaseInsensitive(name).size() != 0;
        }

        /**
         * Test if the request has a header with this value (case insensitive).
         */
        public boolean hasHeaderWithValue(String name, String value) {
           /* Validate.notEmpty(name);
            Validate.notEmpty(value);*/
            List<String> values = headers(name);
            for (String candidate : values) {
                if (value.equalsIgnoreCase(candidate))
                    return true;
            }
            return false;
        }

        public T removeHeader(String name) {
           /* Validate.notEmpty(name, "Header name must not be empty");*/
            Map.Entry<String, List<String>> entry = scanHeaders(name); // remove is case insensitive too
            if (entry != null)
                headers.remove(entry.getKey()); // ensures correct case
            return (T) this;
        }

        public Map<String, String> headers() {
            LinkedHashMap<String, String> map = new LinkedHashMap<>(headers.size());
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                String header = entry.getKey();
                List<String> values = entry.getValue();
                if (values.size() > 0)
                    map.put(header, values.get(0));
            }
            return map;
        }


        public Map<String, List<String>> multiHeaders() {
            return headers;
        }

        private List<String> getHeadersCaseInsensitive(String name) {
           /* Validate.notNull(name);*/

            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                if (name.equalsIgnoreCase(entry.getKey()))
                    return entry.getValue();
            }

            return Collections.emptyList();
        }

        private Map.Entry<String, List<String>> scanHeaders(String name) {
        //    String lc = lowerCase(name);
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
          //      if (lowerCase(entry.getKey()).equals(lc))
                    return entry;
            }
            return null;
        }

        public String cookie(String name) {
            /*Validate.notEmpty(name, "Cookie name must not be empty");*/
            return cookies.get(name);
        }

        public T cookie(String name, String value) {
            /*Validate.notEmpty(name, "Cookie name must not be empty");
            Validate.notNull(value, "Cookie value must not be null");*/
            cookies.put(name, value);
            return (T) this;
        }

        public boolean hasCookie(String name) {
            /*Validate.notEmpty(name, "Cookie name must not be empty");*/
            return cookies.containsKey(name);
        }

        public T removeCookie(String name) {
            /*Validate.notEmpty(name, "Cookie name must not be empty");*/
            cookies.remove(name);
            return (T) this;
        }

        public Map<String, String> cookies() {
            return cookies;
        }
    }

    public static class Request extends Base<HttpConnection.Request>  {
        private Proxy proxy; // nullable
        private int timeoutMilliseconds;
        private int maxBodySizeBytes;
        private boolean followRedirects;
        private Collection<HttpConnection.KeyVal> data;
        private String body = null;
        private boolean ignoreHttpErrors = false;
        private boolean ignoreContentType = false;
        private Parser parser;
        private boolean parserDefined = false; // called parser(...) vs initialized in ctor
        private String postDataCharset = defaultCharset;
        private SSLSocketFactory sslSocketFactory;

        Request() {
            timeoutMilliseconds = 30000; // 30 seconds
            maxBodySizeBytes = 1024 * 1024; // 1MB
            followRedirects = true;
            data = new ArrayList<>();
            method = Method.GET;
            addHeader("Accept-Encoding", "gzip");
            addHeader(USER_AGENT, DEFAULT_UA);
            parser = Parser.htmlParser();
        }

        public Proxy proxy() {
            return proxy;
        }

        public Request proxy(Proxy proxy) {
            this.proxy = proxy;
            return this;
        }

        public Request proxy(String host, int port) {
            this.proxy = new Proxy(Proxy.Type.HTTP, InetSocketAddress.createUnresolved(host, port));
            return this;
        }

        public int timeout() {
            return timeoutMilliseconds;
        }

        public Request timeout(int millis) {
            isTrue(millis >= 0, "Timeout milliseconds must be 0 (infinite) or greater");
            timeoutMilliseconds = millis;
            return this;
        }

        public int maxBodySize() {
            return maxBodySizeBytes;
        }

        public HttpConnection.Request maxBodySize(int bytes) {
            isTrue(bytes >= 0, "maxSize must be 0 (unlimited) or larger");
            maxBodySizeBytes = bytes;
            return this;
        }

        public boolean followRedirects() {
            return followRedirects;
        }

        public HttpConnection.Request followRedirects(boolean followRedirects) {
            this.followRedirects = followRedirects;
            return this;
        }

        public boolean ignoreHttpErrors() {
            return ignoreHttpErrors;
        }

        public SSLSocketFactory sslSocketFactory() {
            return sslSocketFactory;
        }

        public void sslSocketFactory(SSLSocketFactory sslSocketFactory) {
            this.sslSocketFactory = sslSocketFactory;
        }

        public HttpConnection.Request ignoreHttpErrors(boolean ignoreHttpErrors) {
            this.ignoreHttpErrors = ignoreHttpErrors;
            return this;
        }

        public boolean ignoreContentType() {
            return ignoreContentType;
        }

        public HttpConnection.Request ignoreContentType(boolean ignoreContentType) {
            this.ignoreContentType = ignoreContentType;
            return this;
        }

        public Request data(HttpConnection.KeyVal keyval) {
            notNull(keyval, "Key val must not be null");
            data.add(keyval);
            return this;
        }

        public Collection<HttpConnection.KeyVal> data() {
            return data;
        }

        public HttpConnection.Request requestBody(String body) {
            this.body = body;
            return this;
        }

        public String requestBody() {
            return body;
        }

        public Request parser(Parser parser) {
            this.parser = parser;
            parserDefined = true;
            return this;
        }

        public Parser parser() {
            return parser;
        }

        public HttpConnection.Request postDataCharset(String charset) {
          notNull(charset, "Charset must not be null");
            if (!Charset.isSupported(charset)) throw new IllegalCharsetNameException(charset);
            this.postDataCharset = charset;
            return this;
        }

        public String postDataCharset() {
            return postDataCharset;
        }
    }

    public static class Response extends Base<HttpConnection.Response>  {
        private static final int MAX_REDIRECTS = 20;
        private static final String LOCATION = "Location";
        private int statusCode;
        private String statusMessage;
        private ByteBuffer byteData;
        private InputStream bodyStream;
        private HttpURLConnection conn;
        private String charset;
        private String contentType;
        private boolean executed = false;
        private boolean inputStreamRead = false;
        private int numRedirects = 0;
        private HttpConnection.Request req;

        /*
         * Matches XML content types (like text/xml, application/xhtml+xml;charset=UTF8, etc)
         */
        private static final Pattern xmlContentTypeRxp = Pattern.compile("(application|text)/\\w*\\+?xml.*");

        Response() {
            super();
        }

        private Response(Response previousResponse) throws IOException {
            super();
            if (previousResponse != null) {
                numRedirects = previousResponse.numRedirects + 1;
                if (numRedirects >= MAX_REDIRECTS)
                    throw new IOException(String.format("Too many redirects occurred trying to load URL %s", previousResponse.url()));
            }
        }

        static Response execute(HttpConnection.Request req) throws IOException {
            return execute(req, null);
        }

        static Response execute(HttpConnection.Request req, Response previousResponse) throws IOException {
           notNull(req, "Request must not be null");
           notNull(req.url(), "URL must be specified to connect");
            String protocol = req.url().getProtocol();
            if (!protocol.equals("http") && !protocol.equals("https"))
                throw new MalformedURLException("Only http & https protocols supported");
            final boolean methodHasBody = req.method().hasBody();
            final boolean hasRequestBody = req.requestBody() != null;
            if (!methodHasBody)
                isFalse(hasRequestBody, "Cannot set a request body for HTTP method " + req.method());

            // set up the request for execution
            String mimeBoundary = null;
            if (req.data().size() > 0 && (!methodHasBody || hasRequestBody))
                serialiseRequestUrl(req);
            else if (methodHasBody)
                mimeBoundary = setOutputContentType(req);

            long startTime = System.nanoTime();
            HttpURLConnection conn = createConnection(req);
            Response res;
            try {
                conn.connect();
                if (conn.getDoOutput())
                    writePost(req, conn.getOutputStream(), mimeBoundary);

                int status = conn.getResponseCode();
                res = new Response(previousResponse);
                res.setupFromConnection(conn, previousResponse);
                res.req = req;

                // redirect if there's a location header (from 3xx, or 201 etc)
                if (res.hasHeader(LOCATION) && req.followRedirects()) {
                    if (status != HTTP_TEMP_REDIR) {
                        req.method(Method.GET); // always redirect with a get. any data param from original req are dropped.
                        req.data().clear();
                        req.requestBody(null);
                        req.removeHeader(CONTENT_TYPE);
                    }

                    String location = res.header(LOCATION);
                    if (location.startsWith("http:/") && location.charAt(6) != '/') // fix broken Location: http:/temp/AAG_New/en/index.php
                        location = location.substring(6);
                    URL redir = StringUtil.resolve(req.url(), location);
                    req.url(encodeUrl(redir));

                  
                    return execute(req, res);
                }
                if ((status < 200 || status >= 400) && !req.ignoreHttpErrors())
                       System.out.println("HTTP error fetching URL"+ status+ req.url().toString());

                // check that we can handle the returned content type; if not, abort before fetching it
                String contentType = res.contentType();
                if (contentType != null
                        && !req.ignoreContentType()
                        && !contentType.startsWith("text/")
                        && !xmlContentTypeRxp.matcher(contentType).matches()
                        )
                    throw new  IOException("Unhandled content type. Must be text/*, application/xml, or application/xhtml+xml");
                            
            
                res.charset = defaultCharset; // may be null, readInputStream deals with it
                if (conn.getContentLength() != 0 && req.method() != HttpConnection.Method.HEAD) { // -1 means unknown, chunked. sun throws an IO exception on 500 response with no content when trying to read body
                    res.bodyStream = null;
                    res.bodyStream = conn.getErrorStream() != null ? conn.getErrorStream() : conn.getInputStream();
                    if (res.hasHeaderWithValue(CONTENT_ENCODING, "gzip")) {
                        res.bodyStream = new GZIPInputStream(res.bodyStream);
                    } else if (res.hasHeaderWithValue(CONTENT_ENCODING, "deflate")) {
                        res.bodyStream = new InflaterInputStream(res.bodyStream, new Inflater(true));
                    }
                    res.bodyStream = ConstrainableInputStream.wrap(res.bodyStream, bufferSize, req.maxBodySize()).timeout(startTime, req.timeout())
                    ;
                } else {
                    res.byteData = emptyByteBuffer();
                }
            } catch (IOException e){
                // per Java's documentation, this is not necessary, and precludes keepalives. However in practise,
                // connection errors will not be released quickly enough and can cause a too many open files error.
                conn.disconnect();
                throw e;
            }

            res.executed = true;
            return res;
        }

        public int statusCode() {
            return statusCode;
        }

        public String statusMessage() {
            return statusMessage;
        }

        public String charset() {
            return charset;
        }

        public Response charset(String charset) {
            this.charset = charset;
            return this;
        }

        public String contentType() {
            return contentType;
        }

        public Document parse() throws IOException {
           isTrue(executed, "Request must be executed (with .execute(), .get(), or .post() before parsing response");
            if (byteData != null) { // bytes have been read in to the buffer, parse that
                bodyStream = new ByteArrayInputStream(byteData.array());
                inputStreamRead = false; // ok to reparse if in bytes
            }
            isFalse(inputStreamRead, "Input stream already read and parsed, cannot re-read.");
            Document doc = parseInputStream(bodyStream, charset, url.toExternalForm(), req.parser());
            charset = doc.outputSettings().charset().name(); // update charset from meta-equiv, possibly
            inputStreamRead = true;
            safeClose();
            return doc;
        }

        private void prepareByteData() {
            isTrue(executed, "Request must be executed (with .execute(), .get(), or .post() before getting response body");
            if (byteData == null) {
                isFalse(inputStreamRead, "Request has already been read (with .parse())");
                try {
                    byteData = readToByteBuffer(bodyStream, req.maxBodySize());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    inputStreamRead = true;
                    safeClose();
                }
            }
        }

        public String body() {
            prepareByteData();
            // charset gets set from header on execute, and from meta-equiv on parse. parse may not have happened yet
            String body;
            if (charset == null)
                body = Charset.forName(defaultCharset).decode(byteData).toString();
            else
                body = Charset.forName(charset).decode(byteData).toString();
            ((Buffer)byteData).rewind(); // cast to avoid covariant return type change in jdk9
            return body;
        }

        public byte[] bodyAsBytes() {
            prepareByteData();
            return byteData.array();
        }


        public HttpConnection.Response bufferUp() {
            prepareByteData();
            return this;
        }

        public BufferedInputStream bodyStream() {
            isTrue(executed, "Request must be executed (with .execute(), .get(), or .post() before getting response body");
           isFalse(inputStreamRead, "Request has already been read");
            inputStreamRead = true;
            return ConstrainableInputStream.wrap(bodyStream, bufferSize, req.maxBodySize());
        }

        // set up connection defaults, and details from request
        private static HttpURLConnection createConnection(HttpConnection.Request req) throws IOException {
            final HttpURLConnection conn = (HttpURLConnection) (
                req.url().openConnection() 
                
            );

            conn.setRequestMethod(req.method().name());
            conn.setInstanceFollowRedirects(false); // don't rely on native redirection support
            conn.setConnectTimeout(req.timeout());
            conn.setReadTimeout(req.timeout() / 2); // gets reduced after connection is made and status is read

            if (req.sslSocketFactory() != null && conn instanceof HttpsURLConnection)
                ((HttpsURLConnection) conn).setSSLSocketFactory(req.sslSocketFactory());
            if (req.method().hasBody())
                conn.setDoOutput(true);
           for (Map.Entry<String, List<String>> header : req.multiHeaders().entrySet()) {
                for (String value : header.getValue()) {
                    conn.addRequestProperty(header.getKey(), value);
                }
            }
            return conn;
        }

        /**
         * Call on completion of stream read, to close the body (or error) stream
         */
        private void safeClose() {
            if (conn != null) {
                conn.disconnect();
                conn = null;
            }
            if (bodyStream != null) {
                try {
                    bodyStream.close();
                } catch (IOException e) {
                    // no-op
                } finally {
                    bodyStream = null;
                }
            }
        }

        // set up url, method, header, cookies
        private void setupFromConnection(HttpURLConnection conn, Response previousResponse) throws IOException {
            this.conn = conn;
            method = Method.valueOf(conn.getRequestMethod());
            url = conn.getURL();
            statusCode = conn.getResponseCode();
            statusMessage = conn.getResponseMessage();
            contentType = conn.getContentType();

            Map<String, List<String>> resHeaders = createHeaderMap(conn);
            processResponseHeaders(resHeaders);

            // if from a redirect, map previous response cookies into this response
            if (previousResponse != null) {
                for (Map.Entry<String, String> prevCookie : previousResponse.cookies().entrySet()) {
                    if (!hasCookie(prevCookie.getKey()))
                        cookie(prevCookie.getKey(), prevCookie.getValue());
                }
                previousResponse.safeClose();
            }
        }

        private static LinkedHashMap<String, List<String>> createHeaderMap(HttpURLConnection conn) {
            // the default sun impl of conn.getHeaderFields() returns header values out of order
            final LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
            int i = 0;
            while (true) {
                final String key = conn.getHeaderFieldKey(i);
                final String val = conn.getHeaderField(i);
                if (key == null && val == null)
                    break;
                i++;
                if (key == null || val == null)
                    continue; // skip http1.1 line

                if (headers.containsKey(key))
                    headers.get(key).add(val);
                else {
                    final ArrayList<String> vals = new ArrayList<>();
                    vals.add(val);
                    headers.put(key, vals);
                }
            }
            return headers;
        }

        void processResponseHeaders(Map<String, List<String>> resHeaders) {
            for (Map.Entry<String, List<String>> entry : resHeaders.entrySet()) {
                String name = entry.getKey();
                if (name == null)
                    continue; // http/1.1 line

                List<String> values = entry.getValue();
                if (name.equalsIgnoreCase("Set-Cookie")) {
                    for (String value : values) {
                        if (value == null)
                            continue;
                        TokenQueue cd = new TokenQueue(value);
                        String cookieName = cd.chompTo("=").trim();
                        String cookieVal = cd.consumeTo(";").trim();
                        // ignores path, date, domain, ValidateTLSCertificates et al. req'd?
                        // name not blank, value not null
                        if (cookieName.length() > 0)
                            cookie(cookieName, cookieVal);
                    }
                }
                for (String value : values) {
                    addHeader(name, value);
                }
            }
        }
        static String mimeBoundary() {
            final StringBuilder mime = StringUtil.borrowBuilder();
            final Random rand = new Random();
            for (int i = 0; i < boundaryLength; i++) {
                mime.append(mimeBoundaryChars[rand.nextInt(mimeBoundaryChars.length)]);
            }
            return StringUtil.releaseBuilder(mime);
        }
        private static String setOutputContentType(final HttpConnection.Request req) {
            String bound = null;
            if (req.hasHeader(CONTENT_TYPE)) {
                // no-op; don't add content type as already set (e.g. for requestBody())
                // todo - if content type already set, we could add charset

                // if user has set content type to multipart/form-data, auto add boundary.
                if(req.header(CONTENT_TYPE).contains(MULTIPART_FORM_DATA) &&
                        !req.header(CONTENT_TYPE).contains("boundary")) {
                    bound = mimeBoundary();
                    req.header(CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + bound);
                }

            }
            else if (needsMultipart(req)) {
                bound =mimeBoundary();
                req.header(CONTENT_TYPE, MULTIPART_FORM_DATA + "; boundary=" + bound);
            } else {
                req.header(CONTENT_TYPE, FORM_URL_ENCODED + "; charset=" + req.postDataCharset());
            }
            return bound;
        }

        private static void writePost(final HttpConnection.Request req, final OutputStream outputStream, final String bound) throws IOException {
            final Collection<HttpConnection.KeyVal> data = req.data();
            final BufferedWriter w = new BufferedWriter(new OutputStreamWriter(outputStream, req.postDataCharset()));

            if (bound != null) {
                // boundary will be set if we're in multipart mode
                for (HttpConnection.KeyVal keyVal : data) {
                    w.write("--");
                    w.write(bound);
                    w.write("\r\n");
                    w.write("Content-Disposition: form-data; name=\"");
                    w.write(encodeMimeName(keyVal.key())); // encodes " to %22
                    w.write("\"");
                    if (keyVal.hasInputStream()) {
                        w.write("; filename=\"");
                        w.write(encodeMimeName(keyVal.value()));
                        w.write("\"\r\nContent-Type: ");
                        w.write(keyVal.contentType() != null ? keyVal.contentType() : DefaultUploadType);
                        w.write("\r\n\r\n");
                        w.flush(); // flush
                        crossStreams(keyVal.inputStream(), outputStream);
                        outputStream.flush();
                    } else {
                        w.write("\r\n\r\n");
                        w.write(keyVal.value());
                    }
                    w.write("\r\n");
                }
                w.write("--");
                w.write(bound);
                w.write("--");
            } else if (req.requestBody() != null) {
                // data will be in query string, we're sending a plaintext body
                w.write(req.requestBody());
            }
            else {
                // regular form data (application/x-www-form-urlencoded)
                boolean first = true;
                for (HttpConnection.KeyVal keyVal : data) {
                    if (!first)
                        w.append('&');
                    else
                        first = false;

                    w.write(URLEncoder.encode(keyVal.key(), req.postDataCharset()));
                    w.write('=');
                    w.write(URLEncoder.encode(keyVal.value(), req.postDataCharset()));
                }
            }
            w.close();
        }

       

        // for get url reqs, serialise the data map into the url
        private static void serialiseRequestUrl(HttpConnection.Request req) throws IOException {
            URL in = req.url();
            StringBuilder url = StringUtil.borrowBuilder();
            boolean first = true;
            // reconstitute the query, ready for appends
            url
                .append(in.getProtocol())
                .append("://")
                .append(in.getAuthority()) // includes host, port
                .append(in.getPath())
                .append("?");
            if (in.getQuery() != null) {
                url.append(in.getQuery());
                first = false;
            }
            for (HttpConnection.KeyVal keyVal : req.data()) {
                isFalse(keyVal.hasInputStream(), "InputStream data not supported in URL query string.");
                if (!first)
                    url.append('&');
                else
                    first = false;
                url
                    .append(URLEncoder.encode(keyVal.key(), defaultCharset))
                    .append('=')
                    .append(URLEncoder.encode(keyVal.value(), defaultCharset));
            }
            req.url(new URL(StringUtil.releaseBuilder(url)));
            req.data().clear(); // moved into url as get params
        }
    }

    private static boolean needsMultipart(HttpConnection.Request req) {
        // multipart mode, for files. add the header if we see something with an inputstream, and return a non-null boundary
        for (HttpConnection.KeyVal keyVal : req.data()) {
            if (keyVal.hasInputStream())
                return true;
        }
        return false;
    }

    public static class KeyVal  {
        private String key;
        private String value;
        private InputStream stream;
        private String contentType;

        public static KeyVal create(String key, String value) {
            return new KeyVal().key(key).value(value);
        }

        public static KeyVal create(String key, String filename, InputStream stream) {
            return new KeyVal().key(key).value(filename).inputStream(stream);
        }

        private KeyVal() {}

        public KeyVal key(String key) {
           notEmpty(key, "Data key must not be empty");
            this.key = key;
            return this;
        }

        public String key() {
            return key;
        }

        public KeyVal value(String value) {
            notNull(value, "Data value must not be null");
            this.value = value;
            return this;
        }

        public String value() {
            return value;
        }

        public KeyVal inputStream(InputStream inputStream) {
            notNull(value, "Data input stream must not be null");
            this.stream = inputStream;
            return this;
        }

        public InputStream inputStream() {
            return stream;
        }

        public boolean hasInputStream() {
            return stream != null;
        }


        public HttpConnection.KeyVal contentType(String contentType) {
           notEmpty(contentType);
            this.contentType = contentType;
            return this;
        }


        public String contentType() {
            return contentType;
        }

        @Override
        public String toString() {
            return key + "=" + value;
        }
    }
    private static class BomCharset {
        private final String charset;
        private final boolean offset;

        public BomCharset(String charset, boolean offset) {
            this.charset = charset;
            this.offset = offset;
        }
    }
}
