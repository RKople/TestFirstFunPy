package fr.shabbattv;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public final class PlexClient {
    public static final class Pin { public long id; public String code; public String token; }

    public static final class Server {
        public String name, accessToken, machineId;
        public boolean owned;
        public JSONArray connections;
    }

    public static final class Movie {
        public String title, year, ratingKey, thumb, partKey;
        public long durationMs;
        public JSONObject json() {
            JSONObject o = new JSONObject();
            try {
                o.put("title", title);
                o.put("year", year);
                o.put("ratingKey", ratingKey);
                o.put("thumb", thumb);
                o.put("partKey", partKey);
                o.put("durationMs", durationMs);
            } catch (Exception ignored) {}
            return o;
        }
    }

    public static final class StreamOption {
        public String id="", label="", language="", languageCode="", key="", codec="";
        public boolean selected, forced;
        public JSONObject json() {
            JSONObject o=new JSONObject();
            try {
                o.put("id",id); o.put("label",label); o.put("language",language); o.put("languageCode",languageCode);
                o.put("key",key); o.put("codec",codec); o.put("selected",selected); o.put("forced",forced);
            } catch(Exception ignored) {}
            return o;
        }
    }

    public static final class PlaybackOptions {
        public final List<StreamOption> audio = new ArrayList<>();
        public final List<StreamOption> subtitles = new ArrayList<>();
    }

    private PlexClient() {}

    private static void headers(Context c, HttpURLConnection h, String token) {
        h.setRequestProperty("Accept", "application/json");
        h.setRequestProperty("X-Plex-Product", "Shabbat TV");
        h.setRequestProperty("X-Plex-Version", "1.5");
        h.setRequestProperty("X-Plex-Client-Identifier", AppState.clientId(c));
        h.setRequestProperty("X-Plex-Platform", "Android TV");
        h.setRequestProperty("X-Plex-Device-Name", "Shabbat TV");
        if (token != null && !token.isEmpty()) h.setRequestProperty("X-Plex-Token", token);
    }

    public static Pin createPin(Context c) throws Exception {
        URL u = new URL("https://plex.tv/api/v2/pins");
        HttpURLConnection h = (HttpURLConnection) u.openConnection();
        h.setRequestMethod("POST"); h.setDoOutput(true); h.setConnectTimeout(15000); h.setReadTimeout(15000);
        headers(c,h,""); h.setFixedLengthStreamingMode(0); try(OutputStream os=h.getOutputStream()){}
        JSONObject j=new JSONObject(read(h));
        Pin p=new Pin(); p.id=j.getLong("id"); p.code=j.getString("code"); p.token=j.optString("authToken",""); return p;
    }

    public static Pin checkPin(Context c,long id)throws Exception{
        HttpURLConnection h=(HttpURLConnection)new URL("https://plex.tv/api/v2/pins/"+id).openConnection();
        h.setConnectTimeout(15000); h.setReadTimeout(15000); headers(c,h,"");
        JSONObject j=new JSONObject(read(h)); Pin p=new Pin(); p.id=id; p.code=j.optString("code",""); p.token=j.optString("authToken",""); return p;
    }

    public static List<Server> listServers(Context c,String accountToken)throws Exception{
        HttpURLConnection h=(HttpURLConnection)new URL("https://plex.tv/api/v2/resources?includeHttps=1&includeRelay=1&includeIPv6=1").openConnection();
        h.setConnectTimeout(15000); h.setReadTimeout(15000); headers(c,h,accountToken);
        JSONArray a=new JSONArray(read(h)); List<Server> out=new ArrayList<>();
        for(int i=0;i<a.length();i++){
            JSONObject r=a.getJSONObject(i); if(!r.optString("provides","").contains("server"))continue;
            JSONArray conns=r.optJSONArray("connections"); if(conns==null||conns.length()==0)continue;
            Server s=new Server(); s.name=r.optString("name","Plex"); s.owned=r.optBoolean("owned",false); s.accessToken=r.optString("accessToken",accountToken); s.machineId=r.optString("clientIdentifier",""); s.connections=conns; out.add(s);
        }
        Collections.sort(out,(a1,b1)->{if(a1.owned!=b1.owned)return a1.owned?-1:1; return a1.name.compareToIgnoreCase(b1.name);});
        return out;
    }

    public static String selectServer(Context c,String accountToken,Server server)throws Exception{
        List<JSONObject> candidates=new ArrayList<>();
        for(int i=0;i<server.connections.length();i++){JSONObject x=server.connections.optJSONObject(i);if(x!=null&&!x.optString("uri","").isEmpty())candidates.add(x);}
        Collections.sort(candidates,(a,b)->Integer.compare(connectionScore(a),connectionScore(b)));
        StringBuilder tried=new StringBuilder(); String selected=null;
        for(JSONObject x:candidates){String uri=x.optString("uri","");if(uri.isEmpty())continue;if(isReachable(c,uri,server.accessToken)){selected=trimSlash(uri);break;}if(tried.length()>0)tried.append(", ");tried.append(uri);}
        if(selected==null)throw new Exception("Serveur inaccessible depuis cette TV"+(tried.length()==0?"":" (routes testées : "+tried+")"));
        AppState.prefs(c).edit().putString("plex_account_token",accountToken).putString("plex_server_token",server.accessToken).putString("plex_server_url",selected).putString("plex_server_name",server.name).putString("plex_server_machine_id",server.machineId).putBoolean("plex_server_owned",server.owned).apply();
        LogStore.add(c,"Plex","Serveur sélectionné : "+server.name); return selected;
    }

    public static void discoverServer(Context c,String accountToken)throws Exception{
        List<Server> servers=listServers(c,accountToken); if(servers.isEmpty())throw new Exception("Aucun serveur Plex trouvé"); Exception last=null;
        for(Server s:servers){try{selectServer(c,accountToken,s);return;}catch(Exception e){last=e;}}
        throw last!=null?last:new Exception("Aucun serveur Plex joignable");
    }

    private static int connectionScore(JSONObject x){String uri=x.optString("uri","");boolean local=x.optBoolean("local",false),relay=x.optBoolean("relay",false);int score=0;if(!uri.startsWith("https://"))score+=20;if(local)score+=30;if(relay)score+=10;return score;}
    private static boolean isReachable(Context c,String base,String token){HttpURLConnection h=null;try{h=(HttpURLConnection)new URL(trimSlash(base)+"/identity").openConnection();h.setConnectTimeout(3500);h.setReadTimeout(3500);headers(c,h,token);int code=h.getResponseCode();if(code<200||code>=400)return false;InputStream in=h.getInputStream();if(in!=null)in.close();return true;}catch(Exception e){return false;}finally{if(h!=null)h.disconnect();}}
    private static String trimSlash(String s){while(s.endsWith("/"))s=s.substring(0,s.length()-1);return s;}

    public static List<Movie> searchMovies(Context c,String query)throws Exception{
        String base=AppState.prefs(c).getString("plex_server_url","");String token=AppState.prefs(c).getString("plex_server_token","");if(base.isEmpty()||token.isEmpty())throw new Exception("Plex non connecté");
        List<String> sections=movieSections(c,base,token);List<Movie> out=new ArrayList<>();String q=query==null?"":query.trim().toLowerCase();
        for(String s:sections){HttpURLConnection h=(HttpURLConnection)new URL(base+"/library/sections/"+s+"/all?type=1&X-Plex-Token="+URLEncoder.encode(token,"UTF-8")).openConnection();h.setConnectTimeout(12000);h.setReadTimeout(20000);parseMovies(h.getInputStream(),q,out);if(out.size()>=120)break;}
        Collections.sort(out,Comparator.comparing(m->m.title==null?"":m.title.toLowerCase()));return out;
    }

    private static List<String> movieSections(Context c,String base,String token)throws Exception{
        HttpURLConnection h=(HttpURLConnection)new URL(base+"/library/sections?X-Plex-Token="+URLEncoder.encode(token,"UTF-8")).openConnection();h.setConnectTimeout(12000);h.setReadTimeout(20000);
        XmlPullParser p=XmlPullParserFactory.newInstance().newPullParser();p.setInput(h.getInputStream(),"UTF-8");List<String> result=new ArrayList<>();int e;
        while((e=p.next())!=XmlPullParser.END_DOCUMENT){if(e==XmlPullParser.START_TAG&&"Directory".equals(p.getName())&&"movie".equals(p.getAttributeValue(null,"type"))){String key=p.getAttributeValue(null,"key");if(key!=null)result.add(key);}}
        return result;
    }

    private static void parseMovies(InputStream in,String q,List<Movie> out)throws Exception{
        XmlPullParser p=XmlPullParserFactory.newInstance().newPullParser();p.setInput(in,"UTF-8");Movie current=null;int e;
        while((e=p.next())!=XmlPullParser.END_DOCUMENT){
            if(e==XmlPullParser.START_TAG&&"Video".equals(p.getName())){String title=p.getAttributeValue(null,"title");if(title==null||(!q.isEmpty()&&!title.toLowerCase().contains(q))){current=null;continue;}current=new Movie();current.title=title;current.year=p.getAttributeValue(null,"year");current.ratingKey=p.getAttributeValue(null,"ratingKey");current.thumb=p.getAttributeValue(null,"thumb");String duration=p.getAttributeValue(null,"duration");try{current.durationMs=duration==null?0L:Long.parseLong(duration);}catch(Exception ignored){current.durationMs=0L;}}
            else if(e==XmlPullParser.START_TAG&&"Part".equals(p.getName())&&current!=null&&current.partKey==null)current.partKey=p.getAttributeValue(null,"key");
            else if(e==XmlPullParser.END_TAG&&"Video".equals(p.getName())&&current!=null){if(current.partKey!=null)out.add(current);current=null;}
        }
    }

    public static PlaybackOptions playbackOptions(Context c,JSONObject movie)throws Exception{
        String base=AppState.prefs(c).getString("plex_server_url","");String token=AppState.prefs(c).getString("plex_server_token","");String ratingKey=movie.optString("ratingKey","");
        if(base.isEmpty()||token.isEmpty()||ratingKey.isEmpty())throw new Exception("Métadonnées Plex indisponibles");
        HttpURLConnection h=(HttpURLConnection)new URL(base+"/library/metadata/"+URLEncoder.encode(ratingKey,"UTF-8")+"?X-Plex-Token="+URLEncoder.encode(token,"UTF-8")).openConnection();h.setConnectTimeout(12000);h.setReadTimeout(20000);
        XmlPullParser p=XmlPullParserFactory.newInstance().newPullParser();p.setInput(h.getInputStream(),"UTF-8");PlaybackOptions out=new PlaybackOptions();boolean inFirstPart=false,seenPart=false;int e;
        while((e=p.next())!=XmlPullParser.END_DOCUMENT){
            if(e==XmlPullParser.START_TAG&&"Part".equals(p.getName())){if(!seenPart){seenPart=true;inFirstPart=true;}else if(inFirstPart){inFirstPart=false;}}
            else if(e==XmlPullParser.END_TAG&&"Part".equals(p.getName())&&inFirstPart){inFirstPart=false;}
            else if(e==XmlPullParser.START_TAG&&"Stream".equals(p.getName())&&inFirstPart){
                String type=p.getAttributeValue(null,"streamType"); if(!"2".equals(type)&&!"3".equals(type))continue;
                StreamOption s=new StreamOption();s.id=nvl(p.getAttributeValue(null,"id"));s.language=nvl(p.getAttributeValue(null,"language"));s.languageCode=nvl(p.getAttributeValue(null,"languageCode"));s.key=nvl(p.getAttributeValue(null,"key"));s.codec=nvl(p.getAttributeValue(null,"codec"));s.selected="1".equals(p.getAttributeValue(null,"selected"));s.forced="1".equals(p.getAttributeValue(null,"forced"));
                String title=nvl(p.getAttributeValue(null,"title"));String display=!s.language.isEmpty()?s.language:(!s.languageCode.isEmpty()?s.languageCode.toUpperCase():"Inconnu");if(!title.isEmpty()&&!title.equalsIgnoreCase(display))display+=" · "+title;if(s.forced)display+=" · forcés";if(!s.codec.isEmpty())display+=" · "+s.codec.toUpperCase();s.label=display;
                if("2".equals(type))out.audio.add(s);else out.subtitles.add(s);
            }
        }
        return out;
    }

    public static String streamUrl(Context c,JSONObject movie)throws Exception{
        String base=AppState.prefs(c).getString("plex_server_url","");String token=AppState.prefs(c).getString("plex_server_token","");String part=movie.optString("partKey","");if(part.isEmpty())throw new Exception("Film Plex sans fichier lisible");return resourceUrl(c,part);
    }

    public static String resourceUrl(Context c,String key)throws Exception{
        String base=AppState.prefs(c).getString("plex_server_url","");String token=AppState.prefs(c).getString("plex_server_token","");if(base.isEmpty()||token.isEmpty())throw new Exception("Plex non connecté");if(key==null||key.isEmpty())throw new Exception("Ressource Plex vide");return base+key+(key.contains("?")?"&":"?")+"X-Plex-Token="+URLEncoder.encode(token,"UTF-8");
    }

    private static String nvl(String s){return s==null?"":s;}
    private static String read(HttpURLConnection h)throws Exception{int code=h.getResponseCode();InputStream in=code>=400?h.getErrorStream():h.getInputStream();if(in==null)throw new Exception("HTTP "+code);BufferedReader r=new BufferedReader(new InputStreamReader(in,StandardCharsets.UTF_8));StringBuilder b=new StringBuilder();String line;while((line=r.readLine())!=null)b.append(line);if(code>=400)throw new Exception("HTTP "+code+" "+b);return b.toString();}
}
