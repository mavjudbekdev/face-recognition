package org.example.face.service;

import org.example.face.model.FaceTemplateEntity;
import org.example.face.dto.ImageComparisonResult;
import org.example.face.util.JDBCUtil;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

import org.opencv.objdetect.CascadeClassifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class ComparisonServices {


    @Value("${spring.similarity.threshold}")
    private static double SIMILARITY_THRESHOLD;

    @Value("${spring.face.cascade.path}")
    private static String FACE_CASCADE_PATH;

    public ImageComparisonResult findMostSimilarImage(String givenImageBase64) throws IOException {
        List<FaceTemplateEntity> storedImageEntities = findAll();

        if (givenImageBase64.isEmpty() || storedImageEntities.isEmpty()) {
            throw new IllegalArgumentException("Image data cannot be empty.");
        }

        Mat givenImage = base64ToMat(givenImageBase64);
        if (givenImage.empty()) {
            throw new IOException("Failed to decode the given image.");
        }

        Rect givenFaceRect = detectFace(givenImage);
        if (givenFaceRect == null) {
            throw new IOException("No face detected in the given image.");
        }

        Mat givenFaceROI = new Mat(givenImage, givenFaceRect);
        Imgproc.cvtColor(givenFaceROI, givenFaceROI, Imgproc.COLOR_BGR2GRAY);
        Mat histGivenFace = calculateHistogram(givenFaceROI);

        double maxSimilarity = Double.MIN_VALUE;
        String mostSimilarImageBase64 = null;
        Long mostSimilarImageId = null;

        for (FaceTemplateEntity storedImageEntity : storedImageEntities) {
            String storedImageBase64 = storedImageEntity.getTemplate2();
            Mat storedImage = base64ToMat(storedImageBase64);
            if (storedImage.empty()) {
                continue;
            }

            Rect storedFaceRect = detectFace(storedImage);
            if (storedFaceRect == null) {
                continue;
            }

            Mat storedFaceROI = new Mat(storedImage, storedFaceRect);
            Imgproc.cvtColor(storedFaceROI, storedFaceROI, Imgproc.COLOR_BGR2GRAY);
            Mat histStoredFace = calculateHistogram(storedFaceROI);

            double similarity = Imgproc.compareHist(histGivenFace, histStoredFace, Imgproc.HISTCMP_CORREL);
            if (similarity > maxSimilarity) {
                maxSimilarity = similarity;
                mostSimilarImageBase64 = storedImageBase64;
                mostSimilarImageId = storedImageEntity.getId();
            }
        }

        if (maxSimilarity < SIMILARITY_THRESHOLD) {
            return null;
        }

        return new ImageComparisonResult(mostSimilarImageBase64, mostSimilarImageId);
    }

    private Mat base64ToMat(String base64Image)  {
        byte[] imageBytes = Base64.getDecoder().decode(base64Image.replace("\"", ""));
        return Imgcodecs.imdecode(new MatOfByte(imageBytes), Imgcodecs.IMREAD_UNCHANGED);
    }

    private Rect detectFace(Mat image) {
        CascadeClassifier faceCascade = new CascadeClassifier(FACE_CASCADE_PATH);
        Mat grayImage = new Mat();
        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.equalizeHist(grayImage, grayImage);
        MatOfRect faceDetections = new MatOfRect();
        faceCascade.detectMultiScale(grayImage, faceDetections, 1.1, 5, 0, new Size(30, 30), new Size());
        Rect[] facesArray = faceDetections.toArray();
        if (facesArray.length == 0) {
            return null;
        }
        return facesArray[0];
    }

    private Mat calculateHistogram(Mat image) {
        Mat hist = new Mat();
        Imgproc.calcHist(List.of(image), new MatOfInt(0), new Mat(), hist, new MatOfInt(256), new MatOfFloat(0, 256));
        Core.normalize(hist, hist, 0, 1, Core.NORM_MINMAX);
        return hist;
    }


    private List<FaceTemplateEntity> findAll() {

        String SELECT_ALL_QUERY = "SELECT id, template2 FROM companyfacefingertemplate ORDER BY id LIMIT 10 OFFSET 0";
        List<FaceTemplateEntity> entities = new ArrayList<>();
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;

        try {
            connection = JDBCUtil.getConnection();
            statement = connection.prepareStatement(SELECT_ALL_QUERY);
            resultSet = statement.executeQuery();

            while (resultSet.next()) {
                long id = resultSet.getLong("id");
                String template2 = resultSet.getString("template2");

                FaceTemplateEntity entity = new FaceTemplateEntity();
                entity.setId(id);
                entity.setTemplate2(template2);

                entities.add(entity);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (resultSet != null) resultSet.close();
                if (statement != null) statement.close();
                if (connection != null) connection.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        return entities;
    }

}
