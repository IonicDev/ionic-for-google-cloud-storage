/*
 * (c) 2017-2019 Ionic Security Inc. By using this code, I agree to the Terms & Conditions
 * (https://dev.ionic.com/use) and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionic.cloudstorage.samples;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import com.google.api.client.util.StringUtils;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.ionic.cloudstorage.gcs.GoogleIonicStorage;
import com.ionic.cloudstorage.gcs.Version;
import com.ionic.sdk.agent.data.MetadataMap;
import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.agent.request.createkey.CreateKeysRequest;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;
import org.apache.commons.io.FileUtils;


public class GCSSampleApp {

    enum Action {
        GETSTRING("getString"),
        GETFILE("getFile"),
        PUTSTRING("putString"),
        PUTFILE("putFile"),
        VERSION("version"),;

        final String str;

        Action(String name) {
            this.str = name;
        }
    }

    public static final boolean useSandbox = true; // if true limit file paths to within user's home
                                                   // dir
    private static String HOME = System.getProperty("user.home");

    private static GoogleIonicStorage initializeGIS(Storage storage)
            throws IOException, IonicException {
        // Create a com.ionic.google.cloud.StorageImpl instance that uses a
        // plaintext persistor for creates and fetches. We use the default instance
        // google storage as the backing service for this object.

        // Load a plain-text device profile (SEP) from disk
        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();

        String sProfilePath =
                Paths.get(HOME + "/.ionicsecurity/profiles.pt").toFile().getCanonicalPath();
        ptPersistor.setFilePath(sProfilePath);

        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(ptPersistor, storage);

        ionicStorage.setIonicMetadataMap(getMetadataMap());

        return ionicStorage;
    }

    static void putFile(String bucketName, String blobName, String filePath,
            KeyAttributesMap attributes, GoogleIonicStorage storage) {
        System.out.println("Putting blob as file in bucket");

        String srcFilePathStr = getCanonicalPathString(filePath);

        if ((srcFilePathStr == null) || (srcFilePathStr.isEmpty())) {
            System.err.println("No filepath specified");
            return;
        }

        // Sandbox within user home
        if ((useSandbox) && (!srcFilePathStr.startsWith(HOME))) {
            System.err.println("Filepath outside of user home");
            return;
        }

        Path srcFilePath = Paths.get(srcFilePathStr);

        if (!Files.exists(srcFilePath)) {
            System.err.println("File " + srcFilePathStr + " does not exist.");
            return;
        }
        if (!Files.isRegularFile(srcFilePath)) {
            System.err.println("File " + srcFilePathStr + " not a file.");
            return;
        }

        // Write the Blob and put it in GCS:
        File file = srcFilePath.toFile();

        if (file.exists() && file.isFile()) {
            // Treat as a file
            byte[] fileContent;
            try {
                fileContent = Files.readAllBytes(srcFilePath);
            } catch (IOException e) {
                System.err.println("IOException reading from: " + srcFilePathStr);
                return;
            }
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, blobName))
                    .setContentType("application/octet-stream").build();
            if (attributes != null) {
                storage.create(blobInfo, fileContent, new CreateKeysRequest.Key("", 1, attributes));
            } else {
                storage.create(blobInfo, fileContent);
            }
        }
    }

    static void putString(String bucketName, String blobName, String objectContent,
            KeyAttributesMap attributes, GoogleIonicStorage storage) {
        System.out.println("Putting blob as string in bucket");

        // Write the Blob and put it in GCS:
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, blobName))
                .setContentType("application/octet-stream").build();
        if (attributes != null) {
            storage.create(blobInfo, objectContent.getBytes(),
                    new CreateKeysRequest.Key("", 1, attributes));
        } else {
            storage.create(blobInfo, objectContent.getBytes());
        }
    }

    static void getFile(String bucketName, String blobName, String destination, Storage storage) {
        String destFilePathStr = getCanonicalPathString(destination);

        if ((destFilePathStr == null) || (destFilePathStr.isEmpty())) {
            System.err.println("No filepath specified");
            return;
        }

        Path destFilePath = null;

        // Sandbox within user home
        if ((useSandbox) && (!destFilePathStr.startsWith(HOME))) {
            System.err.println("Filepath outside of user home");
            return;
        }

        destFilePath = Paths.get(destFilePathStr);

        // Check if file already exists but is not a file (e.g. don't try to overwrite a directory)
        if ((Files.exists(destFilePath)) && (!Files.isRegularFile(destFilePath))) {
            System.err.println("File " + destFilePathStr + " not a file.");
            return;
        }

        try {
            // Safe to delete existing file
            Files.deleteIfExists(destFilePath);
        } catch (IOException e) {
            System.err.println("IOException delete destination: " + destFilePathStr);
            return;
        }

        System.out.println("Getting object as file from bucket");
        byte[] decryptedContent = storage.readAllBytes(bucketName, blobName);
        try {
            FileUtils.writeByteArrayToFile(destFilePath.toFile(), decryptedContent);
        } catch (IOException e) {
            System.err.println(e.getLocalizedMessage());
        }
    }

    static void getString(String bucketName, String blobName, Storage storage) {
        System.out.println("Getting object as string from bucket");
        byte[] decryptedContent = storage.readAllBytes(bucketName, blobName);
        if (decryptedContent != null) {
            System.out.println(new String(decryptedContent));
        }
    }

    public static void main(String[] args) throws IOException {
        int attributesArg = 4;
        // Command Line Processing

        if (args.length == 0) {
            usage();
            return;
        }

        Action action = null;

        for (Action a : Action.values()) {
            if (a.str.equals(args[0])) {
                action = a;
                break;
            }
        }
        if (action == null) {
            usage();
            return;
        } else if (action == Action.VERSION) {
            System.out.println(Version.getFullVersion());
            return;
        }

        if (args.length < 3) {
            usage();
            return;
        }

        // Get bucketName arg
        String bucketName = new String(StringUtils.getBytesUtf8(args[1]));
        // Note: GCSSampleApp does not protect against invalid entry of GCS bucket names
        // Current Rules for naming GCS buckets at:
        // https://

        // Get Object Key arg
        String blobName = new String(StringUtils.getBytesUtf8(args[2]));
        // Note: GCSSampleApp does not protect against invalid entry of GCS blob name
        // Current Rules for specifying GCS Blob names at:
        // https://

        GoogleIonicStorage storage;
        try {
            storage = initializeGIS(StorageOptions.getDefaultInstance().getService());
        } catch (IonicException e) {
            System.err.println("Can't get agent: " + e.getMessage());
            return;
        }

        KeyAttributesMap attributes = null;

        switch (action) {
            case PUTFILE:

                if (args.length >= 4) {
                    String srcFilePath = Paths.get(new String(StringUtils.getBytesUtf8(args[3])))
                            .toFile().getCanonicalPath();

                    // Optional: parse any attributes
                    if (args.length > attributesArg) {
                        attributes = parseAttributes(args[attributesArg]);
                        if (attributes == null) {
                            return;
                        }
                    }

                    putFile(bucketName, blobName, srcFilePath, attributes, storage);
                } else {
                    usage();
                }
                break;
            case PUTSTRING:
                if (args.length >= 4) {
                    String objectContent = new String(StringUtils.getBytesUtf8(args[3]));

                    // Optional: parse any attributes
                    if (args.length > attributesArg) {
                        attributes = parseAttributes(args[attributesArg]);
                        if (attributes == null) {
                            return;
                        }
                    }

                    putString(bucketName, blobName, objectContent, attributes, storage);
                } else {
                    usage();
                }
                break;
            case GETSTRING:
                if (args.length >= 3) {
                    getString(bucketName, blobName, storage);
                } else {
                    usage();
                }
                break;
            case GETFILE:
                if (args.length >= 4) {
                    String destFilePath = Paths.get(new String(StringUtils.getBytesUtf8(args[3])))
                            .toFile().getCanonicalPath();
                    getFile(bucketName, blobName, destFilePath, storage);
                } else {
                    usage();
                }
                break;
            case VERSION:
                System.out.println(Version.getFullVersion());
                break;
        }
    }

    public static MetadataMap getMetadataMap() {
        MetadataMap mApplicationMetadata = new MetadataMap();
        mApplicationMetadata.set("ionic-application-name", "IonicGCSExample");
        mApplicationMetadata.set("ionic-application-version", Version.getFullVersion());
        mApplicationMetadata.set("ionic-client-type", "IPCS GCS");
        mApplicationMetadata.set("ionic-client-version", Version.getFullVersion());

        return mApplicationMetadata;
    }

    public static KeyAttributesMap parseAttributes(String str) {
        KeyAttributesMap ret = new KeyAttributesMap();
        String[] pairs = str.split(",");
        for (String pair : pairs) {
            String[] tuples = pair.split(":");
            ArrayList<String> values = new ArrayList<String>();
            for (int i = 1; i < tuples.length; i++) {
                values.add(tuples[i]);
            }
            ret.put(tuples[0], values);
        }
        return ret;
    }

    public static String getCanonicalPathString(String originalPath) {
        String canonicalPathStr = null;

        try {
            canonicalPathStr = Paths.get(originalPath).toFile().getCanonicalPath();
        } catch (NullPointerException e) {
            System.err.println("Missing original pathname");
        } catch (IOException e) {
            System.err.println("Path IOError");
        }
        return canonicalPathStr;
    }

    private static void usage() {
        System.out.println("Usage: prog <put<x> command> | <get<x> command> | version");
        System.out.println("put<x> commands:");
        System.out.println("\tNOTE: <attributes> for these commands is a list of comma delimited tuples " +
            "with each tuple composed of a key followed by a colon delimited list of values");
        System.out.println( "\t\t<key>:<value>[:<value>]…[,<key>:<value>[:<value>]…]…");
        System.out.println( "\t\tExample: attribute1:value1:value2,attribute2:value3");
        System.out.println("\tputFile <bucketName> <blobName> <fileSourcePath>");
        System.out.println("\tputString <bucketName> <blobName> <contentString>");
        System.out.println("get<x> commands:");
        System.out.println("\tgetFile <bucketName> <blobName> <destinationPath");
        System.out.println("\tgetString <bucketName> <blobName>");
    }
}
