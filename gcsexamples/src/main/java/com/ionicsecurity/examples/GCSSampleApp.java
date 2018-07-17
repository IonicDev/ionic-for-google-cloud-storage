/*
 * (c) 2017-2018 Ionic Security Inc.
 * By using this code, I agree to the Terms & Conditions (https://dev.ionic.com/use.html)
 * and the Privacy Policy (https://www.ionic.com/privacy-notice/).
 */

package com.ionicsecurity.examples;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;

import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import com.ionic.sdk.agent.key.KeyAttributesMap;
import com.ionic.sdk.device.profile.persistor.DeviceProfilePersistorPlainText;
import com.ionic.sdk.error.IonicException;

import com.ionicsecurity.ipcs.google.GoogleIonicStorage;
import com.ionicsecurity.ipcs.google.Version;


public class GCSSampleApp {
    
    enum Action {
        GETSTRING ("getString"),
        GETFILE ("getFile"),
        PUTSTRING("putString"),
        PUTFILE("putFile"),
        VERSION ("version"),
        ;
        
        final String str;
        
        Action (String name)
        {
            this.str = name;
        }
    }
    

    private static Storage initializeGIS(Storage storage) throws IonicException {
        // Create a com.ionic.google.cloud.StorageImpl instance that uses a
        // plaintext persistor for creates and fetches. We use the default instance 
        // google storage as the backing service for this object.

        // Load a plain-text device profile (SEP) from disk
        DeviceProfilePersistorPlainText ptPersistor = new DeviceProfilePersistorPlainText();
        String sProfilePath = System.getProperty("user.home") + "/.ionicsecurity/profiles.pt";
        try {
            ptPersistor.setFilePath(sProfilePath);
        } catch (IonicException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        GoogleIonicStorage ionicStorage = new GoogleIonicStorage(ptPersistor, storage);

        // Add some attributes for key creates that can be used to control
        // policy:
        List<String> plaintextAttributeEntry = new ArrayList<String>();
        KeyAttributesMap plaintextAttributes = new KeyAttributesMap();
        plaintextAttributeEntry.add("secret");
        plaintextAttributeEntry.add("keyType1AttributeValue1");
        plaintextAttributes.put("keyType1AttributeField1", plaintextAttributeEntry);
        ionicStorage.setKeyAttributes(plaintextAttributes);

        return ionicStorage;
    }

    static void putFile(String bucketName, String blobName, String filePath, Storage storage) {
        System.out.println("Putting blob " + blobName + " in bucket " + bucketName);

        // Write the Blob and put it in GCS:
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            // Treat as a file
            BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, blobName)).setContentType("application/octet-stream").build();
            try {
                storage.create(blobInfo, FileUtils.openInputStream(file));
            } catch (IOException e) {
                System.err.println(e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }
    
    static void putString(String bucketName, String blobName, String objectContent, Storage storage) {
        System.out.println("Putting blob " + blobName + " in bucket " + bucketName);

        // Write the Blob and put it in GCS:
        BlobInfo blobInfo = BlobInfo.newBuilder(BlobId.of(bucketName, blobName)).setContentType("application/octet-stream").build();
        storage.create(blobInfo, objectContent.getBytes());
    }
    
    static void getFile(String bucketName, String blobName, String destination, Storage storage)
    {
        Path destinationPath = Paths.get(destination);
        if (destinationPath.toFile().exists())
        {
            destinationPath.toFile().delete();
        }
        System.out.println("Getting object '" + blobName + "' at '" + bucketName + "'");
        byte[] decryptedContent = storage.readAllBytes(bucketName, blobName);
        try {
            FileUtils.writeByteArrayToFile(destinationPath.toFile(), decryptedContent);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    static void getString(String bucketName, String blobName, Storage storage) {
        System.out.println("Getting object '" + blobName + "' at '" + bucketName + "'");
        byte[] decryptedContent = storage.readAllBytes(bucketName, blobName);
        if (decryptedContent != null) {
            System.out.println(new String(decryptedContent));
        }
    }
    
    public static void main(String[] args) throws IOException 
    {
        // Command Line Processing
        
        if (args.length == 0)
        {
            usage();
        }
        
        Action action = null;
        
        for ( Action a : Action.values())
        {
            if (a.str.equals(args[0]))
            {
                action = a;
                break;
            }
        }
        if(action == null)
        {
            usage();
        }
        
        // Use our com.ionic.google.cloud.StorageImpl instance as a normal
        // Google Storage implementation for the rest of the code:
        Storage storage;
        try {
            storage = initializeGIS(StorageOptions.getDefaultInstance().getService());
        } catch (IonicException e) {
            System.out.println("Can't get agent: " + e.getMessage());
            return;
        }
        
        switch (action)
        {
            case PUTFILE:

                if (args.length >= 4)
                {
                    putFile(args[1], args[2], args[3], storage);
                } 
                else
                {
                    usage();
                }
                break;
            case PUTSTRING:
                if (args.length >= 4)
                {
                    putString(args[1], args[2], args[3], storage);
                } 
                else
                {
                    usage();
                }
                break;
            case GETSTRING:
                if (args.length >= 3)
                {
                    getString(args[1], args[2], storage);
                }
                else
                {
                    usage();
                }
                break;
            case GETFILE:
                if (args.length >= 4)
                {
                    getFile(args[1], args[2], args[3], storage);
                }
                else
                {
                    usage();
                }
                break;
            case VERSION:
                System.out.println(Version.getFullVersion());
                break;
        }
        System.exit(0);
    }
    
    private static void usage() {
        System.out.println("putFile <bucketName> <blobName> <fileSourcePath>");
        System.out.println("putString <bucketName> <blobName> <contentString>");
        System.out.println("getFile <bucketName> <blobName> <destinationPath");
        System.out.println("getString <bucketName> <blobName>");
        System.out.println("version");
        System.exit(1);
    }
}
