package com.chhabinath.file.controller;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.chhabinath.file.payload.UploadFileResponse;
import com.chhabinath.file.service.FileStorageService;

import jakarta.servlet.http.HttpServletRequest;

/**
 * REST controller for handling file upload and download operations.
 * 
 * Base URL: /api/files
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    @Autowired
    private FileStorageService fileStorageService;

    /**
     * Uploads a single file to the server.
     * 
     * @param file The file to be uploaded (sent as multipart/form-data)
     * @return An UploadFileResponse containing file details and download URI
     */
    @PostMapping("/upload")
    public UploadFileResponse uploadFile(@RequestParam("file") MultipartFile file) {
        // Store the file using service and get the stored file name
        String fileName = fileStorageService.storeFile(file);

        // Build the download URI for the stored file
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/download/")
                .path(fileName)
                .toUriString();

        // Return a response containing file info and download link
        return new UploadFileResponse(
                fileName,
                fileDownloadUri,
                file.getContentType(),
                file.getSize());
    }

    /**
     * Downloads a file from the server.
     * 
     * @param fileName The name of the file to be downloaded
     * @param request  HttpServletRequest to determine file's content type
     * @return ResponseEntity containing the file as a Resource
     */
    @GetMapping("/download/{fileName:.+}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String fileName,
            HttpServletRequest request) {

        // Load file as a Spring Resource (from file system)
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        // Try to determine the file's content type
        String contentType = null;
        try {
            contentType = request
                    .getServletContext()
                    .getMimeType(resource.getFile().getAbsolutePath());
        } catch (IOException ex) {
            System.out.println("Could not determine file type.");
        }

        // Default to binary stream if content type is unknown
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        // Return file in response with appropriate headers
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
}
