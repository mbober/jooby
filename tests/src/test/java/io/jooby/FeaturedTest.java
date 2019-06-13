package io.jooby;

import io.jooby.jetty.Jetty;
import io.jooby.json.Jackson;
import io.jooby.netty.Netty;
import io.jooby.utow.Utow;
import io.reactivex.Flowable;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Scanner;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

import static io.jooby.MediaType.text;
import static io.jooby.MediaType.xml;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static okhttp3.RequestBody.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static reactor.core.scheduler.Schedulers.elastic;

public class FeaturedTest {

  private static String _19kb = readText(
      userdir("src", "test", "resources", "files", "19kb.txt"));

  private static String _16kb = _19kb.substring(0, ServerOptions._16KB);

  private static String _8kb = _16kb.substring(0, _16kb.length() / 2);

  private static MediaType json = MediaType.parse("application/json");

  private static MediaType textplain = MediaType.parse("text/plain");

  public static class Datafile {

    public final String name;

    public final Path filename;

    public Datafile(String name, Path filename) {
      this.name = name;
      this.filename = filename;
    }
  }

  public static class Datafiles {

    public final String name;

    public final List<FileUpload> filename;

    public Datafiles(String name, List<FileUpload> filename) {
      this.name = name;
      this.filename = filename;
    }
  }

  @Test
  public void sayHi() {
    new JoobyRunner(app -> {

      app.get("/", ctx -> "Hello World!");

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });
    });

  }

  @Test
  public void sayHiFromIO() {
    new JoobyRunner(app -> {
      app.get("/", ctx -> "Hello World!");
      app.dispatch(() -> {
        app.get("/worker", ctx -> "Hello World!");
      });
    }).mode(ExecutionMode.EVENT_LOOP).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });

      client.get("/worker", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });

      client.get("/favicon.ico", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(0, rsp.body().contentLength());
      });

      client.get("/notFound", rsp -> {
        assertEquals("<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<meta charset=\"utf-8\">\n"
            + "<style>\n"
            + "body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n"
            + "h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n"
            + "h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n"
            + "footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n"
            + "hr {background-color: #f7f7f9;}\n"
            + "div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n"
            + "p {padding-left: 20px;}\n"
            + "p.tab {padding-left: 40px;}\n"
            + "</style>\n"
            + "<title>Not Found (404)</title>\n"
            + "<body>\n"
            + "<h1>Not Found</h1>\n"
            + "<hr>\n<h2>status code: 404</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(580, rsp.body().contentLength());
      });
    }, Jetty::new);
  }

  @Test
  public void sayHiFromWorker() {
    new JoobyRunner(app -> {
      app.get("/", ctx -> "Hello World!");
      app.dispatch(() -> {
        app.get("/worker", ctx -> "Hello World!");
      });
    }).mode(ExecutionMode.WORKER).ready(client -> {
      client.get("/?foo=bar", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });

      client.get("/worker", rsp -> {
        assertEquals("Hello World!", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(12, rsp.body().contentLength());
      });

      client.get("/favicon.ico", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(0, rsp.body().contentLength());
      });

      client.get("/notFound", rsp -> {
        assertEquals("<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<meta charset=\"utf-8\">\n"
            + "<style>\n"
            + "body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n"
            + "h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n"
            + "h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n"
            + "footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n"
            + "hr {background-color: #f7f7f9;}\n"
            + "div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n"
            + "p {padding-left: 20px;}\n"
            + "p.tab {padding-left: 40px;}\n"
            + "</style>\n"
            + "<title>"
            + "Not Found (404)"
            + "</title>\n"
            + "<body>\n"
            + "<h1>Not Found</h1>\n"
            + "<hr>\n<h2>status code: 404</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
        assertEquals(404, rsp.code());
        assertEquals(580, rsp.body().contentLength());
      });
    });
  }

  @Test
  public void rawPath() {
    new JoobyRunner(app -> {

      app.get("/{code}", ctx -> ctx.pathString());

    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(4, rsp.body().contentLength());
      });

      client.get("/a+b", rsp -> {
        assertEquals("/a+b", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(4, rsp.body().contentLength());
      });

      client.get("/%2F", rsp -> {
        assertEquals("/%2F", rsp.body().string());
        assertEquals(200, rsp.code());
        assertEquals(4, rsp.body().contentLength());
      });
    });
  }

  @Test
  public void httpMethods() {
    new JoobyRunner(app -> {

      app.get("/", ctx -> ctx.getMethod());

      app.post("/", ctx -> ctx.getMethod());

      app.put("/", ctx -> ctx.getMethod());

      app.delete("/", ctx -> ctx.getMethod());

      app.patch("/", ctx -> ctx.getMethod());

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("GET", rsp.body().string());
      });

      client.post("/", rsp -> {
        assertEquals("POST", rsp.body().string());
      });

      client.put("/", rsp -> {
        assertEquals("PUT", rsp.body().string());
      });

      client.delete("/", rsp -> {
        assertEquals("DELETE", rsp.body().string());
      });

      client.put("/", rsp -> {
        assertEquals("PUT", rsp.body().string());
      });

      client.patch("/", rsp -> {
        assertEquals("PATCH", rsp.body().string());
      });

    });
  }

  @Test
  public void gzip() throws IOException {
    String text =
        "Praesent blandit, justo a luctus elementum, ante sapien pellentesque tortor, "
            + "vitae maximus nulla augue sed nulla. Phasellus quis turpis ac mi tristique aliquam. "
            + "Suspendisse tellus sem, sollicitudin ac elit id, laoreet viverra erat. Proin libero "
            + "nulla, efficitur at facilisis a, placerat in ligula. Quisque sem dolor, efficitur ac "
            + "nisl ut, porttitor iaculis leo. Nullam auctor neque augue, quis malesuada nisl rhoncus "
            + "id. Quisque ut odio erat. Mauris pellentesque suscipit libero et laoreet. Nulla ipsum "
            + "enim, sodales in risus egestas, tempus pulvinar erat. Sed elementum, leo eget vulputate "
            + "commodo, ligula eros ullamcorper risus, at feugiat neque ipsum quis lectus. Nulla sit "
            + "amet lectus lacinia, congue sapien ac, vehicula lacus. Vestibulum vitae vestibulum enim. "
            + "Proin vulputate, quam ut commodo pellentesque, enim tortor ornare neque, a aliquam massa "
            + "felis a ligula. Pellentesque lorem erat, fringilla at ipsum a, scelerisque hendrerit "
            + "lorem. Sed interdum nibh at ante consequat, vitae fermentum augue luctus.";
    new JoobyRunner(app -> {
      app.setServerOptions(new ServerOptions().setGzip(true));
      app.get("/gzip", ctx -> text);
    }).ready(client -> {
      client.get("/gzip").prepare(req -> {
        req.addHeader("Accept-Encoding", "gzip");
      }).execute(rsp -> {
        int min = 525;
        int max = 532;
        ResponseBody body = rsp.body();
        long len = body.contentLength();
        assertTrue(len == min || len == max, "Content-Length:" + len);
        assertEquals("gzip", rsp.header("content-encoding"));
        assertEquals(text, ungzip(body.bytes()));
      });

      // No ContentNegotiation-Encoding
      client.get("/gzip").prepare(req -> {
        // NOTE: required bc okhttp always set ContentNegotiation-Encoding to gzip
        req.addHeader("Accept-Encoding", "");
      }).execute(rsp -> {
        ResponseBody body = rsp.body();
        assertEquals(text, new String(body.bytes()));
        assertEquals(1013, body.contentLength());
        assertEquals(null, rsp.header("content-encoding"));
      });
    });
  }

  private String ungzip(byte[] buff) throws IOException {
    GZIPInputStream gzip = new GZIPInputStream(new ByteArrayInputStream(buff));
    Scanner scanner = new Scanner(gzip);
    StringBuilder str = new StringBuilder();
    while (scanner.hasNext()) {
      str.append(scanner.nextLine());
    }
    return str.toString();
  }

  @Test
  public void pathVariable() {
    new JoobyRunner(app -> {
      app.get("/articles/{id}", ctx -> ctx.path("id").intValue());

      app.get("/articles/*", ctx -> ctx.path("*").value());

      app.get("/file/*path", ctx -> ctx.path("path").value());

      app.get("/catchallWithVarPrefix/{id}/*path",
          ctx -> ctx.path("id").value() + ":" + ctx.path("path").value());

      app.get("/regex/{nid:[0-9]+}", ctx -> ctx.path("nid").intValue());
      app.get("/regex/{zid:[0-9]+}/edit", ctx -> ctx.path("zid").intValue());

      app.get("/file/{file}.json", ctx -> ctx.path("file").value() + ".JSON");

      app.get("/file/{file}.{ext}",
          ctx -> ctx.path("file").value() + "." + ctx.path("ext").value());

      app.get("/profile/{pid}", ctx -> ctx.path("pid").value());

      app.get("/profile/me", ctx -> "me!");

    }).ready(client -> {
      client.get("/articles/123", rsp -> {
        assertEquals("123", rsp.body().string());
      });

      client.get("/articles/tail/match", rsp -> {
        assertEquals("tail/match", rsp.body().string());
      });

      client.get("/catchallWithVarPrefix/55/js/index.js", rsp -> {
        assertEquals("55:js/index.js", rsp.body().string());
      });

      client.get("/file/js/index.js", rsp -> {
        assertEquals("js/index.js", rsp.body().string());
      });

      client.get("/regex/678", rsp -> {
        assertEquals("678", rsp.body().string());
      });

      client.get("/regex/678/edit", rsp -> {
        assertEquals("678", rsp.body().string());
      });

      client.get("/regex/678d", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/file/foo.js", rsp -> {
        assertEquals("foo.js", rsp.body().string());
      });

      client.get("/file/foo.json", rsp -> {
        assertEquals("foo.JSON", rsp.body().string());
      });

      client.get("/profile/me", rsp -> {
        assertEquals("me!", rsp.body().string());
      });

      client.get("/profile/edgar", rsp -> {
        assertEquals("edgar", rsp.body().string());
      });
    });
  }

  @Test
  public void paramKeys() {
    new JoobyRunner(app -> {
      app.get("/articles/{id}", ctx -> ctx.getRoute().getPathKeys());

      app.get("/articles/*", ctx -> ctx.getRoute().getPathKeys());

      app.get("/file/*path", ctx -> ctx.getRoute().getPathKeys());

      app.get("/regex/{nid:[0-9]+}", ctx -> ctx.getRoute().getPathKeys());
      app.get("/regex/{zid:[0-9]+}/edit", ctx -> ctx.getRoute().getPathKeys());

      app.get("/file/{file}.json", ctx -> ctx.getRoute().getPathKeys());

      app.get("/file/{file}.{ext}", ctx -> ctx.getRoute().getPathKeys());

      app.get("/profile/{pid}", ctx -> ctx.getRoute().getPathKeys());

      app.get("/profile/me", ctx -> ctx.getRoute().getPathKeys());

    }).ready(client -> {
      client.get("/articles/123", rsp -> {
        assertEquals("[id]", rsp.body().string());
      });

      client.get("/articles/tail/match", rsp -> {
        assertEquals("[*]", rsp.body().string());
      });

      client.get("/file/js/index.js", rsp -> {
        assertEquals("[path]", rsp.body().string());
      });

      client.get("/regex/678", rsp -> {
        assertEquals("[nid]", rsp.body().string());
      });

      client.get("/regex/678/edit", rsp -> {
        assertEquals("[zid]", rsp.body().string());
      });

      client.get("/file/foo.js", rsp -> {
        assertEquals("[file, ext]", rsp.body().string());
      });

      client.get("/file/foo.json", rsp -> {
        assertEquals("[file]", rsp.body().string());
      });

      client.get("/profile/me", rsp -> {
        assertEquals("[]", rsp.body().string());
      });

      client.get("/profile/edgar", rsp -> {
        assertEquals("[pid]", rsp.body().string());
      });
    });
  }

  @Test
  public void pathEncoding() {
    new JoobyRunner(app -> {
      app.get("/{value}", ctx -> ctx.pathString() + "@" + ctx.path("value").value());
    }).ready(client -> {
      client.get("/a+b", rsp -> {
        assertEquals("/a+b@a+b", rsp.body().string());
      });
      client.get("/a b", rsp -> {
        assertEquals("/a%20b@a b", rsp.body().string());
      });
      client.get("/%2F%2B", rsp -> {
        assertEquals("/%2F%2B@/+", rsp.body().string());
      });
    });
  }

  @Test
  public void queryString() {
    new JoobyRunner(app -> {
      app.get("/", ctx -> ctx.queryString() + "@" + ctx.query("q").value(""));

      app.get("/value", ctx -> ctx.query("user"));
    }).ready(client -> {
      client.get("/?q=a+b", rsp -> {
        assertEquals("?q=a+b@a b", rsp.body().string());
      });
      client.get("/", rsp -> {
        assertEquals("@", rsp.body().string());
      });

      client.get("/value?user.name=user&user.pass=pwd", rsp -> {
        assertEquals("{name=user, pass=pwd}", rsp.body().string());
      });
    });
  }

  @Test
  public void form() {
    new JoobyRunner(app -> {
      app.post("/", ctx -> ctx.form());
    }).mode(ExecutionMode.EVENT_LOOP, ExecutionMode.WORKER).ready(client -> {
      client.post("/", new FormBody.Builder()
          .add("q", "a b")
          .add("user.name", "user")
          .build(), rsp -> {
        assertEquals("{q=a b, user={name=user}}", rsp.body().string());
      });
    });
  }

  @Test
  public void multipart() {
    new JoobyRunner(app -> {
      app.post("/large", ctx -> {
        FileUpload f = ctx.file("f");
        return f.value();
      });

      app.post("/f", ctx -> {
        FileUpload f = ctx.file("f");
        return f.getFileName() + "(type=" + f.getContentType() + ";exists=" + Files
            .exists(f.path())
            + ")";
      });

      app.post("/files", ctx -> {
        List<FileUpload> files = ctx.files("f");
        return files.stream().map(f -> f.getFileName() + "=" + f.getFileSize())
            .collect(Collectors.toList());
      });

      app.post("/datafile", ctx -> {
        Datafile file = ctx.multipart(Datafile.class);
        String result = file.name + "=" + Files.exists(file.filename);
        return result;
      });

      app.post("/datafiles", ctx -> {
        Datafiles file = ctx.multipart(Datafiles.class);
        return file.name + "=" + file.filename;
      });

      app.post("/multipart", ctx -> {
        Map<String, List<String>> multimap = ctx.multipart().toMultimap();
        return multimap;
      });
    }).ready(client -> {
      client.post("/large", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("f", "19kb.txt", create(MediaType.parse("text/plain"), _19kb))
          .build(), rsp -> {
        assertEquals(_19kb, rsp.body().string());
      });

      client.post("/f", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "fileupload.js",
              create(MediaType.parse("application/javascript"),
                  userdir("src", "test", "resources", "files", "fileupload.js").toFile()))
          .build(), rsp -> {
        assertEquals("fileupload.js(type=application/javascript;exists=true)",
            rsp.body().string());
      });

      client.post("/files", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "f1.txt",
              create(MediaType.parse("text/plain"), "text1"))
          .addFormDataPart("f", "f2.txt",
              create(MediaType.parse("text/plain"), "text2"))
          .build(), rsp -> {
        assertEquals("[f1.txt=5, f2.txt=5]", rsp.body().string());
      });

      client.post("/multipart", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("user.name", "user")
          .addFormDataPart("f", "f1.txt",
              create(MediaType.parse("text/plain"), "text1"))
          .addFormDataPart("f", "f2.txt",
              create(MediaType.parse("text/plain"), "text2"))
          .build(), rsp -> {
        assertEquals("{user.name=[user], f=[f1.txt, f2.txt]}", rsp.body().string());
      });

      client.post("/datafile", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("name", "foo_report")
          .addFormDataPart("filename", "fileupload.js",
              create(MediaType.parse("application/javascript"),
                  userdir("src", "test", "resources", "files", "fileupload.js").toFile()))
          .build(), rsp -> {
        assertEquals("foo_report=true", rsp.body().string());
      });

      client.post("/datafiles", new MultipartBody.Builder()
          .setType(MultipartBody.FORM)
          .addFormDataPart("name", "foo_report")
          .addFormDataPart("filename", "f1.txt",
              create(MediaType.parse("text/plain"), "text1"))
          .addFormDataPart("filename", "f2.txt",
              create(MediaType.parse("text/plain"), "text2"))
          .build(), rsp -> {
        assertEquals("foo_report=[f1.txt, f2.txt]", rsp.body().string());
      });
    });
  }

  @Test
  public void beforeAfter() {
    new JoobyRunner(app -> {

      app.before(ctx -> {
        StringBuilder buff = new StringBuilder();
        buff.append("before1:" + ctx.isInIoThread()).append(";");
        ctx.attribute("buff", buff);
      });

      app.after((ctx, value) -> {
        StringBuilder buff = (StringBuilder) value;
        buff.append("after1:" + ctx.isInIoThread()).append(";");
      });

      app.dispatch(() -> {
        app.before(ctx -> {
          StringBuilder buff = ctx.attribute("buff");
          buff.append("before2:" + ctx.isInIoThread()).append(";");
        });

        app.after((ctx, value) -> {
          StringBuilder buff = ctx.attribute("buff");
          buff.append("after2:" + ctx.isInIoThread()).append(";");
        });

        app.get("/", ctx -> {
          StringBuilder buff = ctx.attribute("buff");
          buff.append("result:").append(ctx.isInIoThread()).append(";");
          return buff;
        });
      });

    }).mode(ExecutionMode.EVENT_LOOP).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("before1:false;before2:false;result:false;after2:false;after1:false;",
            rsp.body().string());
      });
    }, Netty::new, Utow::new /* No Jetty bc always use a worker thread */);
  }

  @Test
  public void decorator() {
    new JoobyRunner(app -> {

      app.decorator(next -> ctx -> "{" + ctx.attribute("prefix") + next.apply(ctx) + "}");

      app.before(ctx -> {
        ctx.attribute("prefix", "%");
      });

      app.decorator(next -> ctx -> "<" + ctx.attribute("prefix") + next.apply(ctx) + ">");

      app.get("/decorator", ctx -> ctx.attribute("prefix") + "OK" + "%");

    }).ready(client -> {
      client.get("/decorator", rsp -> {
        assertEquals("{%<%%OK%>}",
            rsp.body().string());
      });
    });
  }

  @Test
  public void parser() {
    new JoobyRunner(app -> {
      app.install(new Jackson());

      app.post("/map", ctx -> ctx.body(Map.class));

      app.post("/ints", ctx -> {
        List<Integer> ints = ctx.body(Reified.list(Integer.class));
        return ints;
      });

      app.post("/str", ctx -> ctx.body().value());
    }).ready((client, server) -> {
      client.header("Content-Type", "application/json");
      client.post("/map", create(json, "{\"foo\": \"bar\"}"), rsp -> {
        assertEquals("{\"foo\":\"bar\"}", rsp.body().string());
      });

      client.header("Content-Type", "application/json");
      client.post("/ints", create(json, "[3, 4, 1]"), rsp -> {
        assertEquals("[3,4,1]", rsp.body().string());
      });

      client.post("/str", create(textplain, _19kb), rsp -> {
        String value = rsp.body().string();
        assertEquals(_19kb, value,
            server.getClass().getSimpleName() + " expected: " + _19kb.length() + ", got: " + value
                .length());
      });
    });
  }

  @Test
  public void jsonVsRawOutput() {
    new JoobyRunner(app -> {
      app.install(new Jackson());

      app.path("/api/pets", () -> {

        app.get("/{id}", ctx -> {
          ctx.setResponseType(io.jooby.MediaType.json);
          return "{\"message\": \"" + ctx.path("id") + "\"}";
        });

        app.get("/raw", ctx -> {
          ctx.setResponseType(io.jooby.MediaType.json);
          return "{\"message\": \"raw\"}".getBytes(StandardCharsets.UTF_8);
        });

        app.get("/", ctx -> {
          return Arrays.asList(mapOf("message", "fooo"));
        });
      });
    }).ready(client -> {
      client.get("/api/pets/fooo", rsp -> {
        assertEquals("application/json;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("{\"message\": \"fooo\"}", rsp.body().string());
      });

      client.get("/api/pets/raw", rsp -> {
        assertEquals("application/json;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("{\"message\": \"raw\"}", rsp.body().string());
      });

      client.get("/api/pets", rsp -> {
        assertEquals("application/json;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("[{\"message\":\"fooo\"}]", rsp.body().string());
      });
    });
  }

  @Test
  public void defaultAndExplicitAcceptableResponses() {
    class Message {
      String value = "OK";

      @Override public String toString() {
        return value;
      }
    }

    new JoobyRunner(app -> {
      app.renderer(io.jooby.MediaType.json, (@Nonnull Context ctx, @Nonnull Object value) ->
          ("{" + value.toString() + "}").getBytes(StandardCharsets.UTF_8)
      );

      app.renderer(io.jooby.MediaType.xml, (@Nonnull Context ctx, @Nonnull Object value) ->
          ("<" + value.toString() + ">").getBytes(StandardCharsets.UTF_8)
      );

      app.get("/defaults", ctx -> {
        ctx.query("type").toOptional().ifPresent(ctx::setResponseType);
        return new Message();
      });

      app.get("/produces", ctx -> new Message())
          .produces(io.jooby.MediaType.json, io.jooby.MediaType.xml);

    }).ready(client -> {
      client.header("Accept", "application/json");
      client.get("/defaults", rsp -> {
        assertEquals("{OK}", rsp.body().string());
      });

      client.header("Accept", "application/xml");
      client.get("/defaults", rsp -> {
        assertEquals("<OK>", rsp.body().string());
      });

      client.header("Accept", "text/plain");
      client.get("/defaults", rsp -> {
        assertEquals("OK", rsp.body().string());
      });

      client.header("Accept", "*/*");
      client.get("/defaults", rsp -> {
        assertEquals("<OK>", rsp.body().string());
      });

      client.header("Accept", "text/html");
      client.get("/defaults", rsp -> {
        assertEquals(406, rsp.code());
      });

      client.header("Accept", "text/html");
      client.get("/defaults?type=text/html", rsp -> {
        assertEquals("OK", rsp.body().string());
      });

      client.header("Accept", "application/json");
      client.get("/produces", rsp -> {
        assertEquals("{OK}", rsp.body().string());
      });

      client.header("Accept", "application/xml");
      client.get("/produces", rsp -> {
        assertEquals("<OK>", rsp.body().string());
      });

      client.header("Accept", "*/*");
      client.get("/produces", rsp -> {
        assertEquals("<OK>", rsp.body().string());
      });

      client.header("Accept", "text/html");
      client.get("/produces", rsp -> {
        assertEquals(406, rsp.code());
      });

      client.header("Accept", "text/plain");
      client.get("/produces", rsp -> {
        assertEquals(406, rsp.code());
      });
    });
  }

  @Test
  public void consumes() {
    new JoobyRunner(app -> {
      app.parser(io.jooby.MediaType.json, new Parser() {
        @Nonnull @Override public String parse(@Nonnull Context ctx, @Nonnull Type type)
            throws Exception {
          return "{" + ctx.body().value() + "}";
        }
      });

      app.parser(xml, new Parser() {
        @Nonnull @Override public String parse(@Nonnull Context ctx, @Nonnull Type type)
            throws Exception {
          return "<" + ctx.body().value() + ">";
        }
      });

      app.get("/defaults", ctx -> {
        return ctx.body(String.class);
      });

      app.get("/consumes", ctx -> ctx.body(String.class))
          .consumes(io.jooby.MediaType.json, io.jooby.MediaType.xml);

    }).ready(client -> {
      client.header("Content-Type", "application/json");
      client.get("/defaults", rsp -> {
        assertEquals("{}", rsp.body().string());
      });
      client.header("Content-Type", "application/xml");
      client.get("/defaults", rsp -> {
        assertEquals("<>", rsp.body().string());
      });
      client.header("Content-Type", "text/plain");
      client.get("/defaults", rsp -> {
        assertEquals("", rsp.body().string());
      });

      client.header("Content-Type", "application/json");
      client.get("/consumes", rsp -> {
        assertEquals("{}", rsp.body().string());
      });
      client.header("Content-Type", "application/xml");
      client.get("/consumes", rsp -> {
        assertEquals("<>", rsp.body().string());
      });
      client.header("Content-Type", "text/plain");
      client.get("/consumes", rsp -> {
        assertEquals(415, rsp.code());
      });
    });
  }

  @Test
  public void errorHandler() {
    new JoobyRunner(app -> {
      app.install(new Jackson());

      app.get("/", ctx -> {
        if (ctx.pathString().length() != 0) {
          throw new IllegalArgumentException("Intentional error");
        }
        return "OK";
      });

    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.body().contentType().toString().toLowerCase());
        assertEquals("<!doctype html>\n"
            + "<html>\n"
            + "<head>\n"
            + "<meta charset=\"utf-8\">\n"
            + "<style>\n"
            + "body {font-family: \"open sans\",sans-serif; margin-left: 20px;}\n"
            + "h1 {font-weight: 300; line-height: 44px; margin: 25px 0 0 0;}\n"
            + "h2 {font-size: 16px;font-weight: 300; line-height: 44px; margin: 0;}\n"
            + "footer {font-weight: 300; line-height: 44px; margin-top: 10px;}\n"
            + "hr {background-color: #f7f7f9;}\n"
            + "div.trace {border:1px solid #e1e1e8; background-color: #f7f7f9;}\n"
            + "p {padding-left: 20px;}\n"
            + "p.tab {padding-left: 40px;}\n"
            + "</style>\n"
            + "<title>Bad Request (400)</title>\n"
            + "<body>\n"
            + "<h1>Bad Request</h1>\n"
            + "<hr>\n"
            + "<h2>message: Intentional error</h2>\n"
            + "<h2>status code: 400</h2>\n"
            + "</body>\n"
            + "</html>", rsp.body().string());
      });

      client.header("Accept", "application/json");
      client.get("/", rsp -> {
        assertEquals("application/json;charset=utf-8",
            rsp.body().contentType().toString().toLowerCase());
        assertEquals(
            "{\"message\":\"Intentional error\",\"statusCode\":400,\"reason\":\"Bad Request\"}",
            rsp.body().string());
      });
    });
  }

  @Test
  public void rx2() {
    new JoobyRunner(app -> {
      app.get("/rx/flowable", ctx ->
          Flowable.range(1, 10)
              .map(i -> i + ",")
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
      );
      app.get("/rx/observable", ctx ->
          Observable.range(1, 10)
              .map(i -> i + ",")
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
      );
      app.get("/rx/single", ctx ->
          Single.fromCallable(() -> "Single")
              .map(s -> "Hello " + s)
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
      );
      app.get("/rx/maybe", ctx ->
          Maybe.fromCallable(() -> "Maybe")
              .map(s -> "Hello " + s)
              .subscribeOn(Schedulers.io())
              .observeOn(Schedulers.computation())
      );

      app.get("/rx/nomaybe", ctx ->
          Maybe.empty()
              .subscribeOn(Schedulers.io())
      );

      app.get("/rx/flowable/each", ctx -> {
        Writer writer = ctx.responseWriter();
        return Flowable.range(1, 10)
            .map(i -> i + ",")
            .subscribeOn(Schedulers.io())
            .observeOn(Schedulers.computation())
            .doOnError(ctx::sendError)
            .doFinally(writer::close)
            .forEach(it -> {
              writer.write(it);
            });
      });
    }).ready(client -> {
      client.get("/rx/flowable", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
      });
      client.get("/rx/observable", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
      });
      client.get("/rx/single", rsp -> {
        assertEquals("Hello Single", rsp.body().string());
      });
      client.get("/rx/maybe", rsp -> {
        assertEquals("Hello Maybe", rsp.body().string());
      });
      client.get("/rx/nomaybe", rsp -> {
        assertEquals(404, rsp.code());
        assertEquals("", rsp.body().string());
      });
      client.get("/rx/flowable/each", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
      });
    });
  }

  @Test
  public void reactor() {
    new JoobyRunner(app -> {
      app.get("/reactor/mono", ctx ->
          Mono.fromCallable(() -> "Mono")
              .map(s -> "Hello " + s)
              .subscribeOn(elastic())
      );
      app.get("/reactor/flux", ctx ->
          Flux.range(1, 10)
              .map(i -> i + ",")
              .subscribeOn(elastic())
      );
    }).ready(client -> {
      client.get("/reactor/mono", rsp -> {
        assertEquals("10", rsp.header("content-length").toLowerCase());
        assertEquals("Hello Mono", rsp.body().string());
      });
      client.get("/reactor/flux", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals("1,2,3,4,5,6,7,8,9,10,", rsp.body().string());
      });
    });
  }

  @Test
  public void completableFuture() {
    new JoobyRunner(app -> {

      app.get("/completable", ctx ->
          supplyAsync(() -> "Completable Future!")
              .thenApply(v -> "Hello " + v)
      );

    }).ready(client -> {
      client.get("/completable", rsp -> {
        assertEquals("Hello Completable Future!", rsp.body().string());
      });
    });
  }

  @Test
  public void basePath() {
    new JoobyRunner(app -> {
      app.setContextPath("/foo");
      app.get("/bar", ctx -> ctx.pathString());

    }).ready(client -> {
      client.get("/foo/bar", rsp -> {
        assertEquals("/foo/bar", rsp.body().string());
      });

      client.get("/foo/barx", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/favicon.ico", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/foo/favicon.ico", rsp -> {
        assertEquals(404, rsp.code());
      });
    });
  }

  @Test
  public void simpleRouterComposition() {
    new JoobyRunner(app -> {

      Jooby bar = new Jooby();
      bar.get("/bar", Context::pathString);

      app.get("/foo", Context::pathString);

      app.use(bar);

    }).ready(client -> {
      client.get("/bar", rsp -> {
        assertEquals("/bar", rsp.body().string());
      });

      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
      });
    });
  }

  @Test
  public void dynamicRoutingComposition() {
    new JoobyRunner(app -> {

      Jooby v1 = new Jooby();
      v1.get("/api", ctx -> "v1");

      Jooby v2 = new Jooby();
      v2.get("/api", ctx -> "v2");

      app.use(ctx -> ctx.header("version").value().equals("v1"), v1);
      app.use(ctx -> ctx.header("version").value().equals("v2"), v2);

    }).ready(client -> {
      client.header("version", "v2");
      client.get("/api", rsp -> {
        assertEquals("v2", rsp.body().string());
      });

      client.header("version", "v1");
      client.get("/api", rsp -> {
        assertEquals("v1", rsp.body().string());
      });
    });
  }

  @Test
  public void prefixPathOnExistingRouter() {
    new JoobyRunner(app -> {

      Jooby bar = new Jooby();
      bar.get("/bar", Context::pathString);

      app.use("/prefix", bar);

    }).ready(client -> {
      client.get("/prefix/bar", rsp -> {
        assertEquals("/prefix/bar", rsp.body().string());
      });
    });
  }

  @Test
  public void compose() {
    new JoobyRunner(app -> {

      Jooby bar = new Jooby();
      bar.get("/bar", Context::pathString);

      app.path("/api/", () -> {
        app.use(bar);

        app.use("/bar", bar);
      });

    }).ready(client -> {
      client.get("/api/bar", rsp -> {
        assertEquals("/api/bar", rsp.body().string());
      });
      client.get("/api/bar/bar", rsp -> {
        assertEquals("/api/bar/bar", rsp.body().string());
      });
    });
  }

  @Test
  public void routerCaseInsensitive() {
    new JoobyRunner(app -> {
      // This is on by default:
      app.getRouterOptions().setCaseSensitive(false);
      app.get("/foo", Context::pathString);

      app.get("/BAR", Context::pathString);
    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
      });
      client.get("/foo/", rsp -> {
        assertEquals("/foo/", rsp.body().string());
      });
      client.get("/fOo", rsp -> {
        assertEquals("/fOo", rsp.body().string());
      });

      client.get("/bar", rsp -> {
        assertEquals("/bar", rsp.body().string());
      });
      client.get("/BAR", rsp -> {
        assertEquals("/BAR", rsp.body().string());
      });
    });

    /** Now do it case sensitive: */
    new JoobyRunner(app -> {

      app.getRouterOptions().setCaseSensitive(true);

      app.get("/foo", Context::pathString);

      app.get("/BAR", Context::pathString);
    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("/foo", rsp.body().string());
      });
      client.get("/fOo", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/bar", rsp -> {
        assertEquals(404, rsp.code());
      });
      client.get("/BAR", rsp -> {
        assertEquals("/BAR", rsp.body().string());
      });
    });
  }

  @Test
  public void maxRequestSize() {
    new JoobyRunner(app -> {
      app.setServerOptions(new ServerOptions()
          .setBufferSize(ServerOptions._16KB / 2)
          .setMaxRequestSize(ServerOptions._16KB));
      app.post("/request-size", ctx -> ctx.body().value());

      app.get("/request-size", ctx -> ctx.body().value());
    }).ready((client, server) -> {
      // Exceeds
      client.post("/request-size", RequestBody.create(MediaType.get("text/plain"), _19kb), rsp -> {
        assertEquals(413, rsp.code(), server.getClass().getSimpleName());
      });
      // Chunk by chunk
      client.post("/request-size", RequestBody.create(MediaType.get("text/plain"), _16kb), rsp -> {
        assertEquals(200, rsp.code());
        assertEquals(_16kb, rsp.body().string(), server.getClass().getSimpleName());
      });
      // Single read
      client.post("/request-size", RequestBody.create(MediaType.get("text/plain"), _8kb), rsp -> {
        assertEquals(200, rsp.code());
        assertEquals(_8kb, rsp.body().string(), server.getClass().getSimpleName());
      });
      // Empty
      client.post("/request-size", RequestBody.create(MediaType.get("text/plain"), ""), rsp -> {
        assertEquals(200, rsp.code());
        assertEquals("", rsp.body().string(), server.getClass().getSimpleName());
      });
      // No body
      client.get("/request-size", rsp -> {
        assertEquals(200, rsp.code());
        assertEquals("", rsp.body().string(), server.getClass().getSimpleName());
      });
    });
  }

  @Test
  public void routerGotOverrideWhenTrailingSlashOff() {
    new JoobyRunner(app -> {
      // This is on by default:
      // app.ignoreTrailingSlash(true);
      app.get("/foo/", ctx -> "foo/");

      app.get("/foo", ctx -> "new foo");
    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("new foo", rsp.body().string());
      });
      client.get("/foo/", rsp -> {
        assertEquals("new foo", rsp.body().string());
      });
    });

    new JoobyRunner(app -> {
      app.getRouterOptions().setIgnoreTrailingSlash(false);
      app.get("/foo/", ctx -> "trailing slash");

      app.get("/foo", ctx -> "no trailing slash");
    }).ready(client -> {
      client.get("/foo", rsp -> {
        assertEquals("no trailing slash", rsp.body().string());
      });
      client.get("/foo/", rsp -> {
        assertEquals("trailing slash", rsp.body().string());
      });
    });
  }

  @Test
  public void methodNotAllowed() {
    new JoobyRunner(app -> {
      app.post("/method", Context::pathString);
    }).ready(client -> {
      client.get("/method", rsp -> {
        assertEquals(StatusCode.METHOD_NOT_ALLOWED.value(), rsp.code());
      });
    });
  }

  @Test
  public void silentFavicon() {
    new JoobyRunner(Jooby::new).ready(client -> {
      client.get("/favicon.ico", rsp -> {
        assertEquals(StatusCode.NOT_FOUND.value(), rsp.code());
      });
      client.get("/foo/favicon.ico", rsp -> {
        assertEquals(StatusCode.NOT_FOUND.value(), rsp.code());
      });
    });
  }

  @Test
  public void customHttpMethod() {
    new JoobyRunner(app -> {
      app.route("foo", "/bar", Context::getMethod);
    }).ready(client -> {
      client.invoke("foo", "/bar").execute(rsp -> {
        assertEquals("FOO", rsp.body().string());
      });
    });
  }

  @Test
  public void setContentType() {
    new JoobyRunner(app -> {
      app.get("/plain", ctx -> ctx.setResponseType(text).send("Text"));
    }).ready(client -> {
      client.get("/plain", rsp -> {
        assertEquals("text/plain;charset=utf-8",
            rsp.body().contentType().toString().toLowerCase());
        assertEquals("Text", rsp.body().string());
      });
    });
  }

  @Test
  public void setContentLen() {
    String value = "...";
    new JoobyRunner(app -> {
      app.get("/len",
          ctx -> ctx.setResponseType(text).setResponseLength(value.length()).send(value));
    }).ready(client -> {
      client.get("/len", rsp -> {
        assertEquals("text/plain;charset=utf-8",
            rsp.body().contentType().toString().toLowerCase());
        assertEquals("...", rsp.body().string());
        assertEquals(3L, rsp.body().contentLength());
      });
    });
  }

  @Test
  public void sendStatusCode() {
    new JoobyRunner(app -> {
      app.get("/statuscode", ctx -> ctx.send(StatusCode.OK));
    }).ready(client -> {
      client.get("/statuscode", rsp -> {
        assertEquals("", rsp.body().string());
        assertEquals(0L, rsp.body().contentLength());
      });
    });
  }

  @Test
  public void mixedMode() {
    new JoobyRunner(app -> {
      app.get("/blocking", ctx -> !ctx.isInIoThread());
      app.get("/reactive", ctx ->
          Single.fromCallable(() -> ctx.isInIoThread())
      );
    }).mode(ExecutionMode.DEFAULT).ready(client -> {
      client.get("/blocking", rsp -> {
        assertEquals("true", rsp.body().string());
      });
      client.get("/reactive", rsp -> {
        assertEquals("true", rsp.body().string());
      });
    }, Netty::new, Utow::new);
  }

  @Test
  public void defaultHeaders() {
    LinkedList<String> servers = new LinkedList<>(Arrays.asList("netty", "utow", "jetty"));
    new JoobyRunner(app -> {
      app.get("/", Context::pathString);
    }).ready(client -> {
      client.get("/", rsp -> {
        assertNotNull(rsp.header("Date"));
        assertEquals(servers.getFirst(), rsp.header("Server"));
        assertEquals("text/plain", rsp.header("Content-Type").toLowerCase());
        servers.removeFirst();
      });
    });
  }

  @Test
  public void sendStream() {
    new JoobyRunner(app -> {
      app.get("/txt", ctx -> {
        ctx.query("l").toOptional().ifPresent(len -> ctx.setResponseLength(Long.parseLong(len)));
        return new ByteArrayInputStream(_19kb.getBytes(StandardCharsets.UTF_8));
      });
    }).ready(client -> {
      client.get("/txt", rsp -> {
        assertEquals("chunked", rsp.header("transfer-encoding").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/txt?l=" + _19kb.length(), rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()), rsp.header("content-length").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });
    });
  }

  @Test
  public void sendStreamRange() {
    new JoobyRunner(app -> {
      app.get("/range", ctx -> {
        ctx.setResponseLength(_19kb.length());
        return ctx.send(new ByteArrayInputStream(_19kb.getBytes(StandardCharsets.UTF_8)));
      });
    }).ready(client -> {
      client.header("Range", "bytes=-");
      client.get("/range", rsp -> {
        assertEquals(416, rsp.code());
      });

      client.header("Range", "bytes=0-99");
      client.get("/range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals("bytes 0-99/18944", rsp.header("content-range"));
        assertEquals("100", rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(0, 100), rsp.body().string());
      });

      client.header("Range", "bytes=-100");
      client.get("/range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals("bytes 18844-18943/18944", rsp.header("content-range"));
        assertEquals("100", rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(_19kb.length() - 100), rsp.body().string());
      });

      int last = _16kb.length() + 5;
      client.header("Range", "bytes=-" + last);
      client.get("/range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals("bytes 2555-18943/18944", rsp.header("content-range"));
        assertEquals(Integer.toString(last), rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(_19kb.length() - last), rsp.body().string());
      });
      client.header("Range", "bytes=0-" + last);
      client.get("/range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals("bytes 0-" + last + "/18944", rsp.header("content-range"));
        assertEquals(Integer.toString(last + 1), rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(0, last + 1), rsp.body().string());
      });
    });
  }

  @Test
  public void sendFile() {
    new JoobyRunner(app -> {
      app.get("/filechannel", ctx ->
          FileChannel.open(userdir("src", "test", "resources", "files", "19kb.txt"))
      );
      app.get("/path", ctx ->
          userdir("src", "test", "resources", "files", "19kb.txt")
      );
      app.get("/file", ctx ->
          userdir("src", "test", "resources", "files", "19kb.txt").toFile()
      );
      app.get("/filenotfound", ctx ->
          userdir("src", "test", "resources", "files", "notfound.txt")
      );
      app.get("/attachment", ctx -> {
        Path file = userdir("src", "test", "resources", "files", "19kb.txt");
        return new AttachedFile(file, ctx.query("name").value(file.getFileName().toString()));
      });
    }).ready(client -> {
      client.get("/filechannel", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/path", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/file", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/filenotfound", rsp -> {
        assertEquals(404, rsp.code());
      });

      client.get("/attachment", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals("text/plain;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("attachment;filename=\"19kb.txt\"",
            rsp.header("content-disposition").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });

      client.get("/attachment?name=foo+bar.txt", rsp -> {
        assertEquals(null, rsp.header("transfer-encoding"));
        assertEquals(Integer.toString(_19kb.length()),
            rsp.header("content-length").toLowerCase());
        assertEquals("text/plain;charset=utf-8", rsp.header("content-type").toLowerCase());
        assertEquals("attachment;filename=\"foo bar.txt\";filename*=utf-8''foo%20bar.txt",
            rsp.header("content-disposition").toLowerCase());
        assertEquals(_19kb, rsp.body().string());
      });
    });
  }

  @Test
  public void sendFileRange() {
    new JoobyRunner(app -> {
      app.get("/file-range", ctx -> {
        ctx.setResponseLength(_19kb.length());
        return ctx
            .send(FileChannel.open(userdir("src", "test", "resources", "files", "19kb.txt")));
      });
    }).ready(client -> {
      client.header("Range", "bytes=0-99");
      client.get("/file-range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals("bytes 0-99/18944", rsp.header("content-range"));
        assertEquals("100", rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(0, 100), rsp.body().string());
      });

      client.header("Range", "bytes=-100");
      client.get("/file-range", rsp -> {
        assertEquals("bytes", rsp.header("accept-ranges"));
        assertEquals("bytes 18844-18943/18944", rsp.header("content-range"));
        assertEquals("100", rsp.header("content-length"));
        assertEquals(206, rsp.code());
        assertEquals(_19kb.substring(_19kb.length() - 100), rsp.body().string());
      });
    });
  }

  @Test
  public void writer() {
    new JoobyRunner(app -> {
      app.get("/19kb", ctx ->
          ctx.responseWriter(writer -> {
            try (StringReader reader = new StringReader(_19kb)) {
              transfer(reader, writer);
            }
          })
      );

      app.get("/16kb", ctx ->
          ctx.responseWriter(writer -> {
            try (StringReader reader = new StringReader(_16kb)) {
              transfer(reader, writer);
            }
          })
      );

      app.get("/8kb", ctx ->
          ctx.responseWriter(writer -> {
            try (StringReader reader = new StringReader(_8kb)) {
              transfer(reader, writer);
            }
          })
      );

    }).ready(client -> {
      // Large responses always send as chunk
      client.get("/19kb", rsp -> {
        assertEquals(_19kb, rsp.body().string());
        assertEquals("chunked", rsp.header("Transfer-Encoding").toLowerCase());
        assertEquals(null, rsp.header("Content-Length"));
      });
      // Responses <= 16kb may send content-length or chunks (server specific), but not both.
      client.get("/16kb", rsp -> {
        assertEquals(_16kb, rsp.body().string());
        AtomicInteger i = new AtomicInteger();
        Optional.ofNullable(rsp.header("Content-Length")).ifPresent(value -> {
          assertEquals("16384", value);
          i.incrementAndGet();
        });
        Optional.ofNullable(rsp.header("Transfer-Encoding")).ifPresent(value -> {
          assertEquals("chunked", value.toLowerCase());
          i.incrementAndGet();
        });
        assertEquals(1, i.get());
      });
      client.get("/8kb", rsp -> {
        assertEquals(_8kb, rsp.body().string());
        AtomicInteger i = new AtomicInteger();
        Optional.ofNullable(rsp.header("Content-Length")).ifPresent(value -> {
          assertEquals("8192", value);
          i.incrementAndGet();
        });
        Optional.ofNullable(rsp.header("Transfer-Encoding")).ifPresent(value -> {
          assertEquals("chunked", value.toLowerCase());
          i.incrementAndGet();
        });
        assertEquals(1, i.get());
      });
    });
  }

  @Test
  public void lifeCycle() {
    AtomicInteger counter = new AtomicInteger();

    Consumer<Jooby> lifeCycle = app -> {
      app.onStarting(() -> counter.incrementAndGet());

      app.onStarting(() -> counter.incrementAndGet());

      app.onStop(() -> {
        counter.decrementAndGet();
      });

      app.onStop(() -> {
        counter.decrementAndGet();
        throw new IllegalStateException("intentional error");
      });

      app.get("/lifeCycle", ctx -> counter.get());
    };

    new JoobyRunner(lifeCycle).ready(client -> {
      client.get("/lifeCycle", rsp ->
          assertEquals("2", rsp.body().string())
      );
    }, Netty::new);

    assertEquals(0, counter.get());

    new JoobyRunner(lifeCycle).ready(client -> {
      client.get("/lifeCycle", rsp ->
          assertEquals("2", rsp.body().string())
      );
    }, Utow::new);

    assertEquals(0, counter.get());

    new JoobyRunner(lifeCycle).ready(client -> {
      client.get("/lifeCycle", rsp ->
          assertEquals("2", rsp.body().string())
      );
    }, Jetty::new);

    assertEquals(0, counter.get());
  }

  @Test
  public void sessionCookie() {
    new JoobyRunner(app -> {

      app.get("/findSession", ctx -> Optional.ofNullable(ctx.sessionOrNull()).isPresent());
      app.get("/getSession", ctx -> ctx.session().get("foo").value("none"));
      app.get("/putSession", ctx -> ctx.session().put("foo", "bar").get("foo").value());
      app.get("/destroySession", ctx -> {
        Session session = ctx.session();
        session.destroy();

        return Optional.ofNullable(ctx.sessionOrNull()).isPresent();
      });

    }).ready(client -> {
      client.get("/findSession", rsp -> {
        assertEquals("[]", rsp.headers("Set-Cookie").toString());
        assertEquals("false", rsp.body().string());
      });
      client.header("Cookie", "jooby.sid=1234missing");
      client.get("/findSession", rsp -> {
        assertEquals("[]", rsp.headers("Set-Cookie").toString());
        assertEquals("false", rsp.body().string());
      });

      client.get("/getSession", rsp -> {
        assertEquals("none", rsp.body().string());
        String sid = sid(rsp, "jooby.sid=");

        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/findSession", findSession -> {
          assertEquals("[]", findSession.headers("Set-Cookie").toString());
          assertEquals("true", findSession.body().string());
        });
        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/putSession", putSession -> {
          assertEquals("[]", putSession.headers("Set-Cookie").toString());
          assertEquals("bar", putSession.body().string());
        });
        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/getSession", putSession -> {
          assertEquals("[]", putSession.headers("Set-Cookie").toString());
          assertEquals("bar", putSession.body().string());
        });
        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/destroySession", putSession -> {
          assertEquals(
              "[jooby.sid=;Path=/;HttpOnly;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00 GMT]",
              putSession.headers("Set-Cookie").toString());
          assertEquals("false", putSession.body().string());
        });
        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/findSession", putSession -> {
          assertEquals("[]", putSession.headers("Set-Cookie").toString());
          assertEquals("false", putSession.body().string());
        });
      });
    });

    /**********************************************************************************************/
    // Max Age
    /**********************************************************************************************/
    new JoobyRunner(app -> {
      app.setSessionOptions(
          new SessionOptions(SessionId.cookie(new Cookie("my.sid").setMaxAge(1L)))
      );
      app.get("/session", ctx -> ctx.session().toMap());
      app.get("/sessionMaxAge", ctx -> Optional.ofNullable(ctx.sessionOrNull()).isPresent());
    }).ready(client -> {
      client.get("/session", rsp -> {
        String setCookie = rsp.header("Set-Cookie");
        assertTrue(setCookie.startsWith("my.sid"));
        assertTrue(setCookie.contains(";Max-Age=1;"));
      });
    });
  }

  @Test
  public void signSession() {
    new JoobyRunner(app -> {
      app.setSessionOptions(new SessionOptions("987654345!$009P"));
      app.get("/findSession", ctx -> Optional.ofNullable(ctx.sessionOrNull()).isPresent());
      app.get("/getSession", ctx -> ctx.session().get("foo").value("none"));
      app.get("/putSession", ctx -> ctx.session().put("foo", "bar").get("foo").value());
      app.get("/destroySession", ctx -> {
        Session session = ctx.session();
        session.destroy();

        return Optional.ofNullable(ctx.sessionOrNull()).isPresent();
      });

    }).ready(client -> {
      client.get("/findSession", rsp -> {
        assertEquals("[]", rsp.headers("Set-Cookie").toString());
        assertEquals("false", rsp.body().string());
      });
      client.header("Cookie", "jooby.sid=1234missing");
      client.get("/findSession", rsp -> {
        assertEquals("[]", rsp.headers("Set-Cookie").toString());
        assertEquals("false", rsp.body().string());
      });

      client.get("/getSession", rsp -> {
        assertEquals("none", rsp.body().string());
        String sid = sid(rsp, "jooby.sid=");

        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/findSession", findSession -> {
          assertEquals("[]", findSession.headers("Set-Cookie").toString());
          assertEquals("true", findSession.body().string());
        });
        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/putSession", putSession -> {
          assertEquals("[]", putSession.headers("Set-Cookie").toString());
          assertEquals("bar", putSession.body().string());
        });
        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/getSession", putSession -> {
          assertEquals("[]", putSession.headers("Set-Cookie").toString());
          assertEquals("bar", putSession.body().string());
        });
        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/destroySession", putSession -> {
          assertEquals(
              "[jooby.sid=;Path=/;HttpOnly;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00 GMT]",
              putSession.headers("Set-Cookie").toString());
          assertEquals("false", putSession.body().string());
        });
        client.header("Cookie", "jooby.sid=" + sid);
        client.get("/findSession", putSession -> {
          assertEquals("[]", putSession.headers("Set-Cookie").toString());
          assertEquals("false", putSession.body().string());
        });
      });
    });
  }

  @Test
  public void sessionHeader() {
    new JoobyRunner(app -> {

      app.setSessionOptions(new SessionOptions(SessionId.header("jooby.sid")));

      app.get("/findSession", ctx -> Optional.ofNullable(ctx.sessionOrNull()).isPresent());
      app.get("/getSession", ctx -> ctx.session().get("foo").value("none"));
      app.get("/putSession", ctx -> ctx.session().put("foo", "bar").get("foo").value());
      app.get("/destroySession", ctx -> {
        Session session = ctx.session();
        session.destroy();

        return Optional.ofNullable(ctx.sessionOrNull()).isPresent();
      });

    }).ready(client -> {
      client.get("/findSession", rsp -> {
        assertEquals(null, rsp.header("jooby.sid"));
        assertEquals("false", rsp.body().string());
      });
      client.header("jooby.sid", "1234missing");
      client.get("/findSession", rsp -> {
        assertEquals(null, rsp.header("jooby.sid"));
        assertEquals("false", rsp.body().string());
      });

      client.get("/getSession", rsp -> {
        assertEquals("none", rsp.body().string());
        String sid = rsp.header("jooby.sid");

        client.header("jooby.sid", sid);
        client.get("/findSession", findSession -> {
          assertEquals(sid, findSession.header("jooby.sid"));
          assertEquals("true", findSession.body().string());
        });
        client.header("jooby.sid", sid);
        client.get("/putSession", putSession -> {
          assertEquals(sid, putSession.header("jooby.sid"));
          assertEquals("bar", putSession.body().string());
        });
        client.header("jooby.sid", sid);
        client.get("/getSession", putSession -> {
          assertEquals(sid, putSession.header("jooby.sid"));
          assertEquals("bar", putSession.body().string());
        });
        client.header("jooby.sid", sid);
        client.get("/destroySession", putSession -> {
          assertEquals(null, putSession.header("jooby.sid"));
          assertEquals("false", putSession.body().string());
        });
        client.header("jooby.sid", sid);
        client.get("/findSession", putSession -> {
          assertEquals(null, putSession.header("jooby.sid"));
          assertEquals("false", putSession.body().string());
        });
      });
    });
  }

  private String sid(Response rsp, String prefix) {
    String setCookie = rsp.header("Set-Cookie");
    assertNotNull(setCookie);
    assertTrue(setCookie.startsWith(prefix));
    return setCookie.substring(prefix.length(), setCookie.indexOf(";"));
  }

  @Test
  public void sendRedirect() {
    new JoobyRunner(app -> {
      app.get("/", ctx -> ctx.sendRedirect("/foo"));
      app.get("/foo", Context::pathString);
    }).ready(client -> {
      client.get("/", rsp -> {
        assertEquals("/foo", rsp.body().string());
      });
    });
  }

  @Test
  public void cookies() {
    DateTimeFormatter fmt = DateTimeFormatter
        .ofPattern("EEE, dd-MMM-yyyy HH:mm:ss z", Locale.US)
        .withZone(ZoneId.of("GMT"));

    new JoobyRunner(app -> {
      app.get("/cookies", ctx -> ctx.cookieMap());

      app.get("/set-cookie", ctx -> {
        ctx.queryMap().entrySet().stream()
            .map(e -> new Cookie(e.getKey(), e.getValue()))
            .forEach(ctx::setResponseCookie);
        return ctx.send(StatusCode.OK);
      });

      app.get("/max-age", ctx -> {
        Cookie cookie = new Cookie("foo", "bar")
            .setMaxAge(ctx.query("maxAge").longValue(0));
        return ctx
            .setResponseCookie(cookie)
            .send(StatusCode.OK);
      });
    }).ready(client -> {
      client.get("/cookies", response -> {
        assertEquals("{}", response.body().string());
      });
      client.header("Cookie", "foo=bar");
      client.get("/cookies", response -> {
        assertEquals("{foo=bar}", response.body().string());
      });
      client.header("Cookie", "foo=bar; x=y");
      client.get("/cookies", response -> {
        assertEquals("{foo=bar, x=y}", response.body().string());
      });
      client.header("Cookie", "$Version=1; X=x; $Path=/set;");
      client.get("/cookies", response -> {
        assertEquals("{X=x}", response.body().string());
      });
      // response cookies
      client.get("/set-cookie", response -> {
        assertEquals(null, response.header("Set-Cookie"));
      });
      client.get("/set-cookie?foo=bar", response -> {
        assertEquals("foo=bar;Path=/", response.header("Set-Cookie"));
      });
      client.get("/set-cookie?foo=bar&x=y", response -> {
        assertEquals("[foo=bar;Path=/, x=y;Path=/]", response.headers("Set-Cookie").toString());
      });
      // max-age
      client.get("/max-age", response -> {
        // expires
        assertEquals("[foo=bar;Path=/;Max-Age=0;Expires=Thu, 01-Jan-1970 00:00:00 GMT]",
            response.headers("Set-Cookie").toString());
      });
      client.get("/max-age?maxAge=-1", response -> {
        // browser session
        assertEquals("[foo=bar;Path=/]", response.headers("Set-Cookie").toString());
      });
      Instant date = Instant.now();
      client.get("/max-age?maxAge=" + Duration.ofMinutes(30).getSeconds(), response -> {
        // Expire in 30 minutes from now on
        String setCookie = response.header("Set-Cookie");
        String prefix = "foo=bar;Path=/;";
        assertTrue(setCookie.startsWith(prefix));
        setCookie = setCookie.substring(prefix.length());
        String maxAge = "Max-Age=1800;Expires=";
        assertTrue(setCookie.startsWith(maxAge));
        setCookie = setCookie.substring(maxAge.length());
        Instant expires = Instant.from(fmt.parse(setCookie));
        long minutes = Duration.between(date, expires).toMinutes();
        // Give it -/+5
        assertTrue(minutes >= 25 && minutes <= 35);
      });
    });
  }

  @Test
  public void assets() {
    new JoobyRunner(app -> {
      app.assets("/static", userdir("src", "test", "www", "index.html"));
      app.assets("/static/*", userdir("src", "test", "www"));
      app.assets("/*", userdir("src", "test", "www"));
      app.assets("/cp/*", "/www");
      app.assets("/jar/*", "/META-INF/resources/webjars/vue/2.5.22");
      app.assets("/jar2/*", "/META-INF/resources/webjars/vue/2.5.22/dist");

      app.assets("/m/*", AssetSource.create(userdir("src", "test", "www")),
          AssetSource.create(getClass().getClassLoader(), "/www"));
    }).ready(client -> {
      /** Multiple sources on same path: */
      client.get("/m/foo.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals("41", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/m/js/index.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals("23", rsp.header("Content-Length").toLowerCase());
        assertEquals(200, rsp.code());

        String etag = rsp.header("etag");
        client.header("If-None-Match", etag);
        client.get("/m/js/index.js", etagrsp -> {
          assertEquals(null, etagrsp.header("etag"));
          assertEquals(304, etagrsp.code());
        });

        String lastModified = rsp.header("Last-Modified");
        client.header("If-Modified-Since", lastModified);
        client.get("/m/js/index.js", rsp2 -> {
          assertEquals(null, rsp2.header("Last-Modified"));
          assertEquals(304, rsp2.code());
        });
      });
      /** Project classpath: */
      client.get("/cp/foo.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals("41", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/cp", rsp -> {
        assertEquals(404, rsp.code());
      });
      client.get("/cp/", rsp -> {
        assertEquals(404, rsp.code());
      });
      /** File system: */
      client.get("/", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals("155", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals("155", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static/index.html", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals("155", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static/js/index.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals("23", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static/css/styles.css", rsp -> {
        assertEquals("text/css;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals("136", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/static/../resources/fileupload.js", rsp -> {
        assertEquals(404, rsp.code());
      });

      /* ROOT: */
      client.get("/index.html", rsp -> {
        assertEquals("text/html;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals("155", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/js/index.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals("23", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/css/styles.css", rsp -> {
        assertEquals("text/css;charset=utf-8", rsp.header("Content-Type").toLowerCase());
        assertEquals("136", rsp.header("Content-Length").toLowerCase());
      });

      // Inside jar
      client.get("/jar/dist/vue.js", rsp -> {
        assertEquals("application/javascript;charset=utf-8",
            rsp.header("Content-Type").toLowerCase());
        assertEquals("310837", rsp.header("Content-Length").toLowerCase());
      });
      client.get("/jar2/dist/../package.json", rsp -> {
        assertEquals(404, rsp.code());
      });
      client.get("/jar/dist/nope.js", rsp -> {
        assertEquals(404, rsp.code());
      });
      client.get("/jar/dist", rsp -> {
        assertEquals(404, rsp.code());
      });
    });
  }

  @Test
  public void services() {
    new JoobyRunner(app -> {
      ServiceRegistry services = app.getServices();
      services.put(ServiceKey.key(String.class, "x"), "value");

      app.get("/services", ctx -> app.require(String.class, "x"));
    }).ready(client -> {
      client.get("/services", rsp -> {
        assertEquals("value", rsp.body().string());
      });
    });

  }

  @Test
  public void flashScope() {
    new JoobyRunner(app -> {
      app.get("/flash", req -> req.flashMap());

      app.post("/flash", ctx -> {
        ctx.flash("success", "Thank you!");
        return ctx.sendRedirect("/flash");
      });

      app.post("/untouch", Context::pathString);

      app.error((ctx, cause, statusCode) -> {
        ctx.setResponseCode(statusCode).send(cause.getMessage());
      });

    }).dontFollowRedirects().ready(client -> {
      client.post("/flash", rsp -> {
        assertEquals(302, rsp.code());
        String setCookie = rsp.header("Set-Cookie");
        assertEquals("jooby.flash=success=Thank+you%21;Path=/;HttpOnly", setCookie);

        client.header("Cookie", setCookie).get("/flash", next -> {
          assertEquals(200, next.code());
          assertEquals("{success=Thank you!}", next.body().string());
          String clearCookie = next.header("Set-Cookie");
          assertTrue(clearCookie.startsWith("jooby.flash=;Path=/;HttpOnly;Max-Age=0;"),
              clearCookie);
        });
      });

      client.get("/flash", rsp -> {
        assertEquals("{}", rsp.body().string());
        assertNull(rsp.header("Set-Cookie"));
      });

      client.get("/untouch", rsp ->
          assertNull(rsp.header("Set-Cookie"))
      );
    });
  }

  @Test
  public void customFlashScope() {
    new JoobyRunner(app -> {
      app.setContextPath("/custom");
      app.setFlashCookie("f");

      app.get("/flash", ctx -> {
        ctx.flash("success", "Thank you!");
        return ctx.flashMap();
      });
    }).ready(client -> {
      client.get("/custom/flash", rsp -> {
        assertEquals(200, rsp.code());
        String setCookie = rsp.header("Set-Cookie");
        assertEquals("f=success=Thank+you%21;Path=/custom;HttpOnly", setCookie);
      });
    });
  }

  @Test
  public void flashScopeKeep() {
    new JoobyRunner(app -> {
      app.get("/flash", ctx -> {
        FlashMap flash = ctx.flashMap();
        flash.put("foo", "bar");
        return ctx.sendRedirect("/flash/1");
      });

      app.get("/flash/1", ctx -> {
        FlashMap flash = ctx.flashMap();
        flash.keep();
        return ctx.sendRedirect("/flash/" + flash.get("foo"));
      });

      app.get("/flash/bar", ctx -> {
        return ctx.flash("foo").value() + ctx.flashMap().size();
      });
      app.error((ctx, cause, statusCode) -> {
        ctx.setResponseCode(statusCode).send(cause.getMessage());
      });

    }).dontFollowRedirects().ready(client -> {
      client.get("/flash", rsp -> {
        assertEquals(302, rsp.code());
        client.header("Cookie", rsp.header("Set-Cookie")).get(rsp.header("Location"), r1 -> {
          assertEquals(302, r1.code());
          assertEquals("/flash/bar", r1.header("Location"));

          client.header("Cookie", r1.header("Set-Cookie")).get(r1.header("Location"), r2 -> {
            assertEquals(200, r2.code());
            assertEquals("bar1", r2.body().string());

            String clearCookie = r2.header("Set-Cookie");
            assertTrue(clearCookie.startsWith("jooby.flash=;Path=/;HttpOnly;Max-Age=0;"),
                clearCookie);
          });
        });
      });
    });
  }

  private static String readText(Path file) {
    try {
      return new String(Files.readAllBytes(file), StandardCharsets.UTF_8);
    } catch (IOException x) {
      throw Sneaky.propagate(x);
    }
  }

  private static Path userdir(String... segments) {
    Path path = Paths.get(System.getProperty("user.dir"));
    for (String segment : segments) {
      path = path.resolve(segment);
    }
    return path;
  }

  public long transfer(Reader reader, Writer out) throws IOException {
    long transferred = 0;
    char[] buffer = new char[ServerOptions._16KB];
    int nRead;
    while ((nRead = reader.read(buffer, 0, ServerOptions._16KB)) >= 0) {
      out.write(buffer, 0, nRead);
      transferred += nRead;
    }
    return transferred;
  }

  private Map<String, Object> mapOf(String... values) {
    Map<String, Object> hash = new HashMap<>();
    for (int i = 0; i < values.length; i += 2) {
      hash.put(values[i], values[i + 1]);
    }
    return hash;
  }
}
