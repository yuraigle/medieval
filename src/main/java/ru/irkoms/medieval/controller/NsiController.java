package ru.irkoms.medieval.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.irkoms.medieval.controller.dto.SingleMessageDto;
import ru.irkoms.medieval.nsi.FfomsFedPackUpdaterService;
import ru.irkoms.medieval.nsi.FfomsNsiUpdaterService;
import ru.irkoms.medieval.nsi.RmzNsiUpdaterService;

import java.util.List;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
public class NsiController {

    private final FfomsFedPackUpdaterService ffomsFedPackUpdaterService;
    private final FfomsNsiUpdaterService ffomsNsiUpdaterService;
    private final RmzNsiUpdaterService rmzNsiUpdaterService;

    @PostMapping(value = "/api/nsi/update-all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SingleMessageDto> updateAll() {
        try {
            List<String> results1 = ffomsFedPackUpdaterService.updateNsiFedPack();
            List<String> results2 = ffomsNsiUpdaterService.updateAll();
            List<String> results3 = rmzNsiUpdaterService.updateAll();
            List<String> allResults = Stream.of(results1, results2, results3)
                    .flatMap(List::stream)
                    .toList();

            if (allResults.isEmpty()) {
                return ResponseEntity.ok(new SingleMessageDto("Everything is up to date"));
            } else {
                String message = String.join("; ", allResults);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new SingleMessageDto(message));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new SingleMessageDto(e.getMessage()));
        }
    }

}
