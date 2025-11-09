package ru.irkoms.medieval.nsi;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.ToString;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Log4j2
@Service
@RequiredArgsConstructor
public class FfomsNsiUpdaterService {

    private final static String versionUrl = "https://nsi.ffoms.ru/data?pageId=refbookList&containerId=refbookList&size=250";
    private final HttpClient httpClient;
    @Value("${app.nsi_dir}")
    private String nsiDir;

    public List<String> updateAll() throws AppException {
        List<String> requiredNsi = new ArrayList<>();
        requiredNsi.add("F002"); // Реестр страховых медицинских организаций
        requiredNsi.add("F032"); // Реестр медицинских организаций

        try {
            Path nsiPath = Paths.get(nsiDir);
            if (!nsiPath.toFile().exists()) {
                Files.createDirectories(nsiPath);
            }

            List<NsiStatsList.NsiStats> stats = findLatestVersions();

            List<String> results = new ArrayList<>();

            for (NsiStatsList.NsiStats nsi : stats) {
                Integer id = nsi.getId();
                String ver = nsi.getUserVersion();
                String code = nsi.getD() != null ? nsi.getD().getCode() : null;
                if (id != null && ver != null && code != null && requiredNsi.contains(code)) {
                    String fn = code + "-" + ver + ".zip";
                    Path nsiZip = Paths.get(nsiPath.toAbsolutePath().toString(), fn);

                    if (!nsiZip.toFile().exists()) {
                        downloadNsi(nsi.getId(), nsiZip.toFile());
                        results.add(code + " updated to " + ver);
                    }
                }
            }

            return results;
        } catch (Exception e) {
            throw new AppException(e.getMessage());
        }
    }

    private List<NsiStatsList.NsiStats> findLatestVersions() throws IOException, InterruptedException {
        HttpRequest versionReq = HttpRequest.newBuilder()
                .uri(URI.create(versionUrl)).GET().build();

        HttpResponse<String> resp = httpClient.send(versionReq, HttpResponse.BodyHandlers.ofString());
        String body = resp.body();

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        NsiStatsList stats = objectMapper.readValue(body, NsiStatsList.class);

        return stats.getList();
    }

    private void downloadNsi(Integer id, File saveAs) throws IOException, InterruptedException {
        String downloadUrl = "https://nsi.ffoms.ru/refbook?type=XML&id=0&version=" + id;
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(downloadUrl)).GET().build();
        httpClient.send(req, HttpResponse.BodyHandlers.ofFile(saveAs.toPath()));

        log.info("{} downloaded.", saveAs.getName());
    }

    @Data
    public static class NsiStatsList {
        private List<NsiStats> list;
        private Integer count;
        private Integer size;
        private Integer page;

        @Data
        @ToString
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class NsiStats {
            private Integer id;
            private D d;

            @JsonProperty("user_version")
            private String userVersion;

            @JsonProperty("last_update")
            @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "dd.MM.yyyy HH:mm")
            private LocalDateTime lastUpdate;

            @Data
            public static class D {
                private String code;
            }
        }
    }

}
