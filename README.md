````markdown
# Storage Microservice

This microservice provides file storage capabilities using MinIO (S3-compatible) and presigned URLs for uploads and downloads.

## Features

- Upload files (PNG, JPEG, PDF, plain text) via multipart/form-data.
- Enforce file type and size restrictions (max 10 MB).
- Generate presigned download URLs valid for configurable duration (default: 60 minutes).
- MinIO (S3) integration with path-style access.

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
````

## Building and Running

### Using Maven

```bash
# Build
mvn clean install

# Run (via plugin)
mvn spring-boot:run

# Or run the packaged JAR
target/storage-service-0.0.1-SNAPSHOT.jar example:
java -jar target/storage-service-0.0.1-SNAPSHOT.jar
```

## Endpoints

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
curl -i http://localhost:8090/storage/download/123e4567-e89b-12d3-a456-426614174000_myfile.pdf
```

## Project Structure

```
src/
├── main/
│   ├── java/com/example/storage_service
│   │   ├── StorageServiceApplication.java
│   │   ├── controller/StorageController.java
│   │   ├── service/StorageService.java
│   │   └── config/MinioConfig.java
│   └── resources/application.properties
└── test/...
```

## Integration Testing

Use Postman to verify the storage microservice within your DMS workflow (Auth and Dept services assumed running):

1. **Register three accounts**

   * **Request**: `POST {{authBaseUrl}}/auth/register`
   * **Headers**: `Content-Type: application/json`
   * **Body (raw JSON)**:

     ```json
     { "email":"alice@dms.com", "password":"pass", "roles":["ROLE_USER"] }
     ```
   * Repeat for `bob@dms.com` (ROLE\_USER) and `admin@dms.com` (ROLE\_ADMIN).

2. **Log in and capture tokens**

   * **Request**: `POST {{authBaseUrl}}/auth/login`
   * **Headers**: `Content-Type: application/json`
   * **Body (raw JSON)**:

     ```json
     { "email":"alice@dms.com", "password":"pass" }
     ```
   * Copy the `token` field from the response into Postman environment variable `aliceToken`. Repeat for `bobToken` and `adminToken`.

3. **Create a Department**

   * **Request**: `POST {{deptBaseUrl}}/departments`
   * **Headers**:

     * `Authorization: Bearer {{adminToken}}`
     * `Content-Type: application/json`
   * **Body (raw JSON)**:

     ```json
     { "name":"HR" }
     ```
   * Save the returned `id` as `deptId`.

4. **Create a Category**

   * **Request**: `POST {{deptBaseUrl}}/categories`
   * **Headers**:

     * `Authorization: Bearer {{adminToken}}`
     * `Content-Type: application/json`
   * **Body (raw JSON)**:

     ```json
     { "name":"Policies" }
     ```
   * Save the returned `id` as `catId`.

5. **Assign Alice to HR**

   * **Request**: `POST {{deptBaseUrl}}/departments/{{deptId}}/users?email=alice@dms.com`
   * **Headers**: `Authorization: Bearer {{adminToken}}`

6. **Upload a file to Storage**

   * **Request**: `POST {{storageUrl}}/storage/upload`
   * **Body (form-data)**:

     * Key: `file`
     * Value: select any small text or PDF file
   * **Note**: no auth header required
   * Save the response field `key` into environment variable `fileKey`.

7. **Create a Document in Docs service**

   * **Request**: `POST {{deptBaseUrl}}/documents`
   * **Headers**:

     * `Authorization: Bearer {{aliceToken}}`
     * `Content-Type: multipart/form-data`
   * **Body (form-data)**:

     * `file`: choose the same file you uploaded
     * `metadata` (Text, Content-Type `application/json`):

       ```json
       {
         "title":"Policy1",
         "categoryId":"{{catId}}",
         "departmentId":"{{deptId}}",
         "userEmail":"alice@dms.com"
       }
       ```
   * Save the returned `id` into `docId`.

8. **List Alice’s documents**

   * **Request**: `GET {{deptBaseUrl}}/documents/user/alice%40dms.com`
   * **Headers**: `Authorization: Bearer {{aliceToken}}`
   * **Expect**: JSON array containing your new document.

9. **List Bob’s documents (should be empty)**

   * **Request**: `GET {{deptBaseUrl}}/documents/user/bob%40dms.com`
   * **Headers**: `Authorization: Bearer {{bobToken}}`
   * **Expect**: `[]`.

10. **Fetch Alice’s metadata by ID**

* **Request**: `GET {{deptBaseUrl}}/documents/{{docId}}`
* **Headers**: `Authorization: Bearer {{aliceToken}}`
* **Expect**: single `DocumentDto` JSON.

11. **Bob tries to fetch Alice’s by ID (404)**

* **Request**: `GET {{deptBaseUrl}}/documents/{{docId}}`
* **Headers**: `Authorization: Bearer {{bobToken}}`
* **Expect**: `404 Not Found`.

12. **Download redirect endpoint**

* **Request**: `GET {{deptBaseUrl}}/documents/{{docId}}/download`
* **Headers**: `Authorization: Bearer {{aliceToken}}`
* **Expect**: `302 Found` with `Location` header pointing to presigned URL. Follow redirect or copy URL into a new GET to verify file contents.

13. **Bob tries the download redirect (404)**

* **Request**: `GET {{deptBaseUrl}}/documents/{{docId}}/download`
* **Headers**: `Authorization: Bearer {{bobToken}}`
* **Expect**: `404 Not Found`.

## Notes

* Ensure MinIO is accessible and the bucket `dms-files` exists.
* Adjust `aws.region` to match your deployment region if using AWS S3.
* Update CORS or security settings as needed for production.

```
```
