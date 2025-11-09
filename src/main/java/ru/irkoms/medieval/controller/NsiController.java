package ru.irkoms.medieval.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.irkoms.medieval.controller.dto.SingleMessageDto;
import ru.irkoms.medieval.nsi.FfomsNsiUpdaterService;
import ru.irkoms.medieval.nsi.RmzNsiUpdaterService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NsiController {

    private final FfomsNsiUpdaterService ffomsNsiUpdaterService;
    private final RmzNsiUpdaterService rmzNsiUpdaterService;

    /**
     * Запрос на скачивание свежего федерального пакета НСИ с ресурса ФФОМС
     *
     * @return SingleMessageDto сообщение о результате
     */
    @PostMapping(value = "/api/nsi/update-ffoms", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SingleMessageDto> updateFfomsNsi() {
        try {
            String ver = ffomsNsiUpdaterService.updateNsi();
            if (ver == null) {
                return ResponseEntity.ok(new SingleMessageDto("Already up to date"));
            } else {
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new SingleMessageDto("Downloaded version " + ver));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new SingleMessageDto(e.getMessage()));
        }
    }

    /**
     * Запрос на скачивание свежих версий НСИ с сайта РосМинЗдрав
     *
     * @return SingleMessageDto сообщение о результате
     */
    @PostMapping(value = "/api/nsi/update-rmz", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<SingleMessageDto> updateRmzNsi() {
        List<String> results;
        try {
            results = rmzNsiUpdaterService.updateAll();

            if (results.isEmpty()) {
                return ResponseEntity.ok(new SingleMessageDto("Already up to date"));
            } else {
                String message = String.join("; ", results);
                return ResponseEntity.status(HttpStatus.CREATED)
                        .body(new SingleMessageDto(message));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(new SingleMessageDto(e.getMessage()));
        }
    }

}
