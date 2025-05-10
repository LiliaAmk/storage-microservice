# Storage Microservice

This microservice handles file storage using MinIO (S3-compatible) and creates URLs for uploads and downloads.

## Features

- Upload files (PNG, JPEG, PDF, text) using multipart/form-data
- File type and size limits (max 10 MB)
- Create download URLs that expire (default: 60 minutes)
- MinIO (S3) integration

## Prerequisites

- Java 17+
- Maven 3.6+
- Running MinIO instance (default endpoint: `http://localhost:9000`)
- AWS SDK region (e.g., `us-east-1`)

## Configuration

All settings are defined in `src/main/resources/application.properties`:

```properties
# MinIO (S3) connection
minio.endpoint=http://localhost:9000
minio.access-key=minioadmin
minio.secret-key=minioadmin
minio.bucket=dms-files

# AWS SDK region
aws.region=us-east-1

# HTTP server port
server.port=8090
```

## Building and Running

### Using Maven

```bash
# Build
mvn clean install

# Run 
mvn spring-boot:run

# Or run the packaged JAR
target/storage-service-0.0.1-SNAPSHOT.jar example:
java -jar target/storage-service-0.0.1-SNAPSHOT.jar
```

## Endpoints

All endpoints are accessible through the API Gateway at `http://localhost:8090/storage/**`.

### POST /storage/upload

* **Description**: Uploads a file.
* **Content-Type**: `multipart/form-data`
* **Request Part**: `file` (the file to upload)
* **Responses**:

  * `201 Created` with JSON `{ "key": "<object-key>" }`
  * `413 Payload Too Large` if file exceeds 10 MB
  * `415 Unsupported Media Type` if content type is not allowed

**Example with `curl`**:

```bash
curl -i -X POST http://localhost:8090/storage/upload \
  -F file=@path/to/file.png
```

### GET /storage/download/{key}

* **Description**: Redirects to a presigned URL for downloading the file.
* **Path Variable**: `key` (object key returned by the upload)
* **Responses**:

  * `302 Found` with `Location` header set to presigned URL

**Example**:

```bash
curl -i http://localhost:8090/storage/download/{file_name}
```

## Notes

* Ensure MinIO is accessible and the bucket `dms-files` exists.
* Adjust `aws.region` to match your deployment region if using AWS S3.
* Update CORS or security settings as needed for production.
