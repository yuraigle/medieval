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
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

@Log4j2
@Service
@RequiredArgsConstructor
public class FfomsNsiUpdaterService {

    private static final String versionUrl = "https://nsi.ffoms.ru/data?pageId=refbookList.refbookList.download&containerId=main&model=DEFAULT&size=1";
    private static final String downloadUrl = "https://nsi.ffoms.ru/fedPack?type=FULL";
    private static final Path nsiDir = Paths.get(".", "nsi-files").toAbsolutePath();
    private final HttpClient httpClient;

    public String updateNsi() throws AppException {
        try {
            if (!nsiDir.toFile().exists()) {
                Files.createDirectories(nsiDir);
            }

            String ver = findLatestFedPackVersion();
            String fn = "FedPack-" + ver + ".zip";
            Path nsiZip = Paths.get(nsiDir.toAbsolutePath().toString(), fn);

            if (!nsiZip.toFile().exists()) {
                downloadNsiFull(nsiZip.toFile());
                return ver;
            }

            return null;
        } catch (Exception e) {
            throw new AppException(e.getMessage());
        }
    }

    private String findLatestFedPackVersion() throws IOException, InterruptedException {
        HttpRequest versionReq = HttpRequest.newBuilder()
                .uri(URI.create(versionUrl)).GET().build();

        HttpResponse<String> versionResp = httpClient
                .send(versionReq, HttpResponse.BodyHandlers.ofString());

        String body = versionResp.body();

        AtomicReference<String> version = new AtomicReference<>("");
        Pattern.compile("version[^0-9]+([0-9]+)").matcher(body)
                .results().findFirst()
                .ifPresent(m -> version.set(m.group(1)));

        return version.get();
    }

    private void downloadNsiFull(File saveAs) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).GET().build();
        httpClient.send(req, HttpResponse.BodyHandlers.ofFile(saveAs.toPath()));

        log.info("{} downloaded.", saveAs.getName());
    }
}
