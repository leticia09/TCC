package com.br.api.service;

import com.asprise.ocr.Ocr;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.cloud.vision.v1.*;
import com.google.protobuf.ByteString;


@Service
public class LeitorService {

    @Value("${leitor.tess.path}")
    private String tessDataPath;

    @Value("${leitor.diretorio.path}")
    private String diretorioPath;

    @Value("${leitor.directory.process.path}")
    private String directoryProcessPath;

    @Value("${leitor.directory.result.path}")
    private String resultPath;

    public List<String> read(String imagemNome) throws TesseractException, IOException {

        List<List<String>> result = new ArrayList<>();
        List<String> resultGoogle = new ArrayList<>();

        List<String> listFiles = listFiles(directoryProcessPath);
        AtomicInteger count = new AtomicInteger();
        //listFiles.forEach(this::processImage);

        listFiles.forEach(file -> {
            String apiGoogleResult = null;
            try {
                count.set(count.get() + 1);
                System.out.println(count+"/"+listFiles.size() + " -> " + file);
                apiGoogleResult = googleCloudApiVision(directoryProcessPath + file);
                resultGoogle.add(file + " - " + apiGoogleResult);
                //String tesseractResult = null;
                //tesseractResult = tesseract(file);
                //resultGoogle.add(file + " - " + tesseractResult.replaceAll("\n", ""));
            } catch (IOException e) {
                e.printStackTrace();
            }

        });

        //result.add("Tesseract:");
        //result.add(tesseractResult);
        //result.add(resultGoogle);

        return resultGoogle;
    }

    private List<String> listFiles(String directoryPath) {
        File directory = new File(directoryPath);

        List<String> fileNameList = new ArrayList<>();

        if (directory.exists() && directory.isDirectory()) {
            File[] files = directory.listFiles();
            if (files != null && files.length > 0) {
                String[] fileNames = new String[files.length];
                for (int i = 0; i < files.length; i++) {
                    fileNames[i] = files[i].getName();
                }
                fileNameList.addAll(Arrays.asList(fileNames));
            }
        }
        return fileNameList;
    }

    private void processImage(String imageName) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        Mat img = Imgcodecs.imread(diretorioPath + imageName);
        Mat grayImg = new Mat();
        Imgproc.cvtColor(img, grayImg, Imgproc.COLOR_BGR2GRAY);
        Mat binaryImg = new Mat();
        Imgproc.threshold(
                grayImg,
                binaryImg,
                0,
                255,
                Imgproc.THRESH_BINARY | Imgproc.THRESH_OTSU);
        Imgcodecs.imwrite(directoryProcessPath+
                imageName+"_OPENCV"+".jpg",binaryImg);
    }


    public String tesseract(String imagemNome) throws TesseractException {
        Tesseract tesseract = new Tesseract();
        tesseract.setDatapath(tessDataPath);
        String arquivo = diretorioPath + imagemNome;
        return tesseract.doOCR(new File(arquivo));
    }


    private String googleCloudApiVision(String imageName) throws IOException {
        return detectText(imageName);
    }

    private String detectText(String filePath) throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();
        String result = "";
        ByteString imgBytes = ByteString.readFrom(new FileInputStream(filePath));
        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);
        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    System.out.format("Error: %s%n", res.getError().getMessage());
                    return null;
                }

                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    result = annotation.getDescription();
                }
            }
        }
        return result;
    }

    public String verify() {
        String feedbackFile = resultPath + "gabarito.txt";
        String googleWith = resultPath + "google-com-process.txt";
        String google = resultPath + "google-sem-process.txt";
        String tesseractWith = resultPath + "tesseract-com-process.txt";
        String tesseract = resultPath + "tesseract-sem-process.txt";

        List<String> feedback = readFile(feedbackFile);
        List<String> textGoogleWith = readFile(googleWith);
        List<String> textGoogle = readFile(google);
        List<String> textTesseractWith = readFile(tesseractWith);
        List<String> textTesseract = readFile(tesseract);

        List<String> feedbackUpperCase = convertToUppercase(feedback);
        List<String> text1UpperCase = convertToUppercase(textGoogleWith);
        List<String> text2UpperCase = convertToUppercase(textGoogle);
        List<String> text3UpperCase = convertToUppercase(textTesseractWith);
        List<String> text4UpperCase = convertToUppercase(textTesseract);


        String result = compareWithFeedback(text1UpperCase, feedbackUpperCase,  "google-com-process.txt");
        String result1 = compareWithFeedback(text2UpperCase, feedbackUpperCase, "google-sem-process.txt");
        String result2 = compareWithFeedback(text3UpperCase, feedbackUpperCase, "tesseract-com-process.txt");
        String result3 = compareWithFeedback(text4UpperCase, feedbackUpperCase, "tesseract-sem-process.txt");


        return result + "\n" + result1 + "\n" + result2 + "\n" + result3;
    }

    private static List<String> readFile(String filePath) {
        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return lines;
    }

    private static List<String> convertToUppercase(List<String> lines) {
        List<String> linesUpperCase = new ArrayList<>();
        for (String line : lines) {
            linesUpperCase.add(line.replace(",", "").replace("\"", "").toUpperCase());
        }
        return linesUpperCase;
    }

    private static String compareWithFeedback(List<String> lines,
                                              List<String> feedback,
                                              String fileName) {
        int correctWords = 0;
        int incorrectWords = 0;
        int incorrectChar = 0;
        int totalCharacters = 0;

        for(int i = 0; i < lines.size(); i ++) {
            String textLine = lines.get(i)
                    .substring(lines.get(i).indexOf("-") + 1).trim();
            String textLineFeedback = feedback.get(i)
                    .substring(feedback.get(i).indexOf("-") + 1).trim();
            totalCharacters += textLine.length();
            if (textLineFeedback.contains(textLine)) {
                correctWords++;
            } else {
                incorrectWords++;
                incorrectChar += compareCharacters(textLine, textLineFeedback);
            }
        }

        double percentageErrorWords = (double) incorrectWords / lines.size() * 100;
        double percentageErrorChar = (double) incorrectChar / totalCharacters * 100;

       return ("Comparando " + fileName + ":" +
               "\nTotal de Palavras corretas: " + correctWords +
               "\nTotal de Palavras incorretas: " + incorrectWords +
               "\nPercentual de erro: " + percentageErrorWords + "%" +
               "\nTotal de characteres incorretos das palavras incorretas: " + incorrectChar +
               "\nTotal de characteres das palavras incorretas: " + totalCharacters +
               "\nPercentual de erro de characteres: " + percentageErrorChar + "%\n");
    }

    private static int compareCharacters(String text,
                                         String feedback) {
        int wrongCharacters = 0;
        int sizeComparison = Math.min(text.length(),
                feedback.length());
        for (int i = 0; i < sizeComparison; i++) {
            if (!text.contains(
                    String.valueOf(feedback.charAt(i)))
            ) {
                wrongCharacters++;
            }
        }
        return wrongCharacters;

    }

}
