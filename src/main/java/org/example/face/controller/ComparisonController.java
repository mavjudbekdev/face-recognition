package org.example.face.controller;


import lombok.RequiredArgsConstructor;
import org.example.face.service.ComparisonServices;
import org.example.face.dto.ImageComparisonResult;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;


@RestController
@RequestMapping("/com")
@RequiredArgsConstructor
public class ComparisonController {

    private final ComparisonServices compareService;

    @PostMapping("/compare")
    public ResponseEntity<?> compareFace(@RequestBody String givenImageBase64) {
        try {
            ImageComparisonResult mostSimilarImage = compareService.findMostSimilarImage(givenImageBase64);
            if (mostSimilarImage != null)
                return ResponseEntity.status(HttpStatus.OK).body(mostSimilarImage);
            else return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Face not found");
        } catch (IOException e) {
            throw new RuntimeException("Error processing the images", e);
        }
    }
}
