package ru.irkoms.medieval.nsi;

import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import ru.irkoms.medieval.domain.ex.AppException;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Log4j2
@Service
@RequiredArgsConstructor
public class RmzNsiUpdaterService {

    private static final Path nsiDir = Paths.get(".", "nsi-files").toAbsolutePath();
    private final HttpClient httpClient;

    public List<String> updateAll() throws AppException {
        final Map<String, String> oidMap = new HashMap<>();
        oidMap.put("V001", "1.2.643.5.1.13.13.11.1070");
        oidMap.put("M001", "1.2.643.5.1.13.13.11.1005");
        oidMap.put("M002", "1.2.643.5.1.13.13.99.2.734");

        List<String> results = new ArrayList<>();

        for (String id : oidMap.keySet()) {
            String ver = updateNsi(id, oidMap.get(id));
            if (ver != null) {
                results.add(id + " updated to " + ver);
            }
        }

        return results;
    }

    private String updateNsi(String id, String oid) throws AppException {
        try {
            if (!nsiDir.toFile().exists()) {
                Files.createDirectories(nsiDir);
            }

            String ver = getLatestVersion(oid);
            String fn = id + "-" + ver + ".zip";
            Path nsiZip = Paths.get(nsiDir.toAbsolutePath().toString(), fn);

            if (!nsiZip.toFile().exists()) {
                downloadNsi(oid, ver, nsiZip.toFile());
                return ver;
            }

            return null;
        } catch (Exception e) {
            throw new AppException(e.getMessage());
        }
    }

    private String getLatestVersion(String oid) throws IOException, InterruptedException, AppException {
        String versionUrl = "https://nsi.rosminzdrav.ru/api/versions?identifier=" + oid;

        HttpRequest versionReq = HttpRequest.newBuilder()
                .uri(URI.create(versionUrl)).GET().build();

        HttpResponse<String> resp = httpClient.send(versionReq, HttpResponse.BodyHandlers.ofString());
        String body = resp.body();

        if (body != null) {
            Pattern versionPattern = Pattern.compile("\"version\":\\s*\"(.*?)\"");
            Matcher matcher = versionPattern.matcher(body);
            if (matcher.find()) {
                return matcher.group(1);
            }
        }

        throw new AppException("RMZ version not found for oid " + oid);
    }

    private void downloadNsi(String oid, String version, File saveAs) throws IOException, InterruptedException {
        String downloadUrl = "https://nsi.rosminzdrav.ru/api/dataFiles/" + oid + "_" + version + "_xml.zip";
        log.info("Downloading {}", downloadUrl);

        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).GET().build();
        httpClient.send(req, HttpResponse.BodyHandlers.ofFile(saveAs.toPath()));

        log.info("{} downloaded.", saveAs.getName());
    }
}
