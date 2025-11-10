/*
 *
 * Copyright 2025 gematik GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * *******
 *
 * For additional notes and disclaimer from gematik and in case of changes by gematik find details in the "Readme" file.
 */
package de.gematik.ti20.simsvc.client.service;

import de.gematik.ti20.simsvc.client.model.card.Application;
import de.gematik.ti20.simsvc.client.model.card.CardImage;
import de.gematik.ti20.simsvc.client.model.card.EGK;
import de.gematik.ti20.simsvc.client.model.card.FileData;
import de.gematik.ti20.simsvc.client.model.card.HPC;
import de.gematik.ti20.simsvc.client.model.card.HPIC;
import de.gematik.ti20.simsvc.client.model.card.Key;
import de.gematik.ti20.simsvc.client.model.card.Pin;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;
import org.xml.sax.InputSource;

/** Service for parsing XML card images. */
@Service
public class CardImageParser {

  /**
   * Parse an XML string into a CardImage object.
   *
   * @param xmlString XML string to parse
   * @return Parsed CardImage
   * @throws JAXBException If parsing fails
   */
  public CardImage parseCardImage(String xmlString) throws JAXBException {
    // First try the standard format
    try {
      // Create JAXBContext with all relevant classes
      JAXBContext context =
          JAXBContext.newInstance(
              CardImage.class,
              EGK.class,
              HPC.class,
              HPIC.class,
              Application.class,
              Application.Containers.class,
              FileData.class,
              Key.class,
              Pin.class);

      Unmarshaller unmarshaller = context.createUnmarshaller();

      // Parse the XML string
      InputSource inputSource = new InputSource(new StringReader(xmlString));
      return (CardImage) unmarshaller.unmarshal(inputSource);
    } catch (JAXBException e) {
      // First check if this is a new format card (Gema5)
      if (xmlString.contains("Produkttyp:")
          && (xmlString.contains("eGK Objektsystem")
              || xmlString.contains("SMC-B Objektsystem")
              || xmlString.contains("HPC Objektsystem"))) {

        // Try with the fallback method specifically for new format cards
        CardImage newFormatCard = convertNewFormatCard(xmlString);
        if (newFormatCard != null) {
          return newFormatCard;
        }
      }

      // If all else fails, try the generic fallback
      CardImage fallbackCard = createFallbackCardFromXml(xmlString);
      if (fallbackCard != null) {
        return fallbackCard;
      }

      // Log error information before giving up
      System.err.println("JAXB parsing error: " + e.getMessage());
      if (e.getLinkedException() != null) {
        System.err.println("Linked exception: " + e.getLinkedException().getMessage());
        e.getLinkedException().printStackTrace();
      }

      throw e;
    }
  }

  /** Convert new format card (Gema5) to CardImage. */
  private CardImage convertNewFormatCard(String xmlString) {
    try {
      CardImage card = new CardImage();
      card.setId("card-" + System.currentTimeMillis());

      // Determine card type from content
      if (xmlString.contains("EGK_") || xmlString.contains("eGK Objektsystem")) {
        EGK egk = new EGK();
        card.setEgk(egk);
        card.setLabel("eGK Card");

        // Create application structure
        EGK.Applications applications = new EGK.Applications();
        egk.setApplications(applications);
        List<Application> appList = new ArrayList<>();
        applications.setApplicationList(appList);

        // Create basic application
        Application app = new Application();
        app.setApplicationId("ESIGN");
        app.setDeactivated(false);
        app.setContainers(new Application.Containers());
        appList.add(app);

        // Extract files
        extractFilesFromNewFormat(xmlString, app);
      } else if (xmlString.contains("SMC_B_") || xmlString.contains("SMC-B Objektsystem")) {
        HPIC hpic = new HPIC();
        card.setHpic(hpic);
        card.setLabel("SMC-B Card");

        // Create application structure
        HPIC.Applications applications = new HPIC.Applications();
        hpic.setApplications(applications);
        List<Application> appList = new ArrayList<>();
        applications.setApplicationList(appList);

        // Create basic application
        Application app = new Application();
        app.setApplicationId("SMC-B");
        app.setDeactivated(false);
        app.setContainers(new Application.Containers());
        appList.add(app);

        // Extract files
        extractFilesFromNewFormat(xmlString, app);
      } else if (xmlString.contains("HPC_") || xmlString.contains("HPC Objektsystem")) {
        HPC hpc = new HPC();
        card.setHpc(hpc);
        card.setLabel("HPC Card");

        // Create application structure
        HPC.Applications applications = new HPC.Applications();
        hpc.setApplications(applications);
        List<Application> appList = new ArrayList<>();
        applications.setApplicationList(appList);

        // Create basic application
        Application app = new Application();
        app.setApplicationId("HPC");
        app.setDeactivated(false);
        app.setContainers(new Application.Containers());
        appList.add(app);

        // Extract files
        extractFilesFromNewFormat(xmlString, app);
      }

      return card;
    } catch (Exception e) {
      System.err.println("Error converting new format card: " + e.getMessage());
      return null;
    }
  }

  /** Extract files from new format XML. */
  private void extractFilesFromNewFormat(String xmlString, Application app) {
    try {
      List<FileData> files = new ArrayList<>();
      app.getContainers().setFiles(files);

      // Extract correct application identifier (AID) for this application
      Pattern aidPattern =
          Pattern.compile("applicationIdentifier\">[{](.*?)[}]</attribute>", Pattern.DOTALL);
      Matcher aidMatcher = aidPattern.matcher(xmlString);

      // Find appropriate AID for this application type
      String defaultAid = null;
      while (aidMatcher.find()) {
        String aid = aidMatcher.group(1);
        if (aid != null) {
          // For EGK cards
          if (app.getApplicationId().equals("ESIGN")
              && (aid.contains("d27600000102") || aid.contains("a0000001635345"))) {
            app.setApplicationId("ESIGN:" + aid);
            defaultAid = aid;
            break;
          }
          // For SMC-B cards
          else if (app.getApplicationId().equals("SMC-B")
              && (aid.contains("d276000143")
                  || aid.contains("d27600014")
                  || aid.contains("a00000016745"))) {
            app.setApplicationId("SMC-B:" + aid);
            defaultAid = aid;
            break;
          }
          // For HPC cards
          else if (app.getApplicationId().equals("HPC")
              && (aid.contains("d27600006601") || aid.contains("d276000066"))) {
            app.setApplicationId("HPC:" + aid);
            defaultAid = aid;
            break;
          }
          // Default case - use the first AID found
          if (defaultAid == null) {
            defaultAid = aid;
          }
        }
      }

      // Use the first AID found if no specific match
      if (defaultAid != null && !app.getApplicationId().contains(":")) {
        app.setApplicationId(app.getApplicationId() + ":" + defaultAid);
      }

      // Extract file IDs
      Pattern fileIdPattern = Pattern.compile("fileIdentifier\">(.*?)</attribute>", Pattern.DOTALL);
      Matcher fileIdMatcher = fileIdPattern.matcher(xmlString);

      List<String> fileIds = new ArrayList<>();
      while (fileIdMatcher.find()) {
        fileIds.add(fileIdMatcher.group(1).toUpperCase());
      }

      // Extract file content - including herstellerspezifisch attributes
      Pattern bodyPattern = Pattern.compile("body\"[^>]*>(.*?)</attribute>", Pattern.DOTALL);
      Matcher bodyMatcher = bodyPattern.matcher(xmlString);

      List<String> bodies = new ArrayList<>();
      while (bodyMatcher.find()) {
        bodies.add(bodyMatcher.group(1));
      }

      // Match file IDs with content
      int minSize = Math.min(fileIds.size(), bodies.size());
      for (int i = 0; i < minSize; i++) {
        FileData file = new FileData();
        file.setFileId(fileIds.get(i));
        file.setName("EF_" + fileIds.get(i));
        file.setData(bodies.get(i));
        files.add(file);
      }

      // If we have more content than IDs, generate IDs
      for (int i = minSize; i < bodies.size(); i++) {
        FileData file = new FileData();
        file.setFileId("EF" + String.format("%02X", i));
        file.setName("EF_AUTO_" + i);
        file.setData(bodies.get(i));
        files.add(file);
      }

      // Add special files for AID matching
      if (defaultAid != null) {
        // Add the AID as a special file to help with SELECT commands
        FileData aidFile = new FileData();
        // Convert hex string to uppercase without curly braces
        String aidFileId = "AID_" + defaultAid.toUpperCase().replace("D2", "d2");
        aidFile.setFileId(aidFileId);
        aidFile.setName("AID_" + defaultAid);
        aidFile.setData(defaultAid);
        files.add(aidFile);

        // Also create default EF.DIR file with AID info
        FileData efDir = new FileData();
        efDir.setFileId("2F00");
        efDir.setName("EF.DIR");
        efDir.setData(defaultAid);
        files.add(efDir);
      }
    } catch (Exception e) {
      System.err.println("Error extracting files: " + e.getMessage());
      e.printStackTrace();
    }
  }

  /**
   * Creates a fallback CardImage by extracting basic information from XML. This is used when full
   * JAXB parsing fails.
   *
   * @param xmlString XML string to parse
   * @return A simplified CardImage or null if extraction fails
   */
  private CardImage createFallbackCardFromXml(String xmlString) {
    try {
      CardImage card = new CardImage();

      // Set a default ID if needed
      card.setId("card-" + System.currentTimeMillis());

      // Extract card type
      if (xmlString.contains("<hpic>")
          || xmlString.contains("SMC_B_")
          || xmlString.contains("SMC-B")) {
        HPIC hpic = new HPIC();
        card.setHpic(hpic);
        card.setLabel("HPIC Card");

        // Extract keys for HPIC
        extractKeysFromXml(xmlString, hpic);
      } else if (xmlString.contains("<hpc>") || xmlString.contains("HPC_")) {
        card.setHpc(new HPC());
        card.setLabel("HPC Card");
      } else if (xmlString.contains("<ehc>")
          || xmlString.contains("<egk>")
          || xmlString.contains("EGK_")
          || xmlString.contains("eGK")) {
        card.setEgk(new EGK());
        card.setLabel("EGK Card");
      } else {
        // Unknown card type, default to EGK
        card.setEgk(new EGK());
        card.setLabel("Unknown Card (defaulting to EGK)");
      }

      return card;
    } catch (Exception e) {
      System.err.println("Failed to create fallback card: " + e.getMessage());
      return null;
    }
  }

  /**
   * Extracts keys from XML string and adds them to the HPIC card. This is a manual extraction for
   * when JAXB parsing fails.
   *
   * @param xmlString The XML string
   * @param hpic The HPIC card to add keys to
   */
  private void extractKeysFromXml(String xmlString, HPIC hpic) {
    try {
      // Create application and container structure
      HPIC.Applications applications = new HPIC.Applications();
      Application app = new Application();
      Application.Containers containers = new Application.Containers();
      List<Key> keys = new ArrayList<>();
      containers.setKeys(keys);
      app.setContainers(containers);

      List<Application> appList = new ArrayList<>();
      appList.add(app);
      applications.setApplicationList(appList);
      hpic.setApplications(applications);

      // Extract keys from XML using regex patterns
      Pattern keyPattern =
          Pattern.compile(
              "<key keyRef=\"(\\d+)\" name=\"([^\"]+)\">\\s*<privateKey>([^<]+)</privateKey>");
      Matcher matcher = keyPattern.matcher(xmlString);

      // Also extract keyIdentifier from XML object definitions
      Pattern keyIdPattern =
          Pattern.compile(
              "<child id=\"([^\"]*(?:PrK|SK)[^\"]*)\">\\s*<attributes>\\s*<attribute"
                  + " id=\"keyIdentifier\">([^<]+)</attribute>");
      Matcher keyIdMatcher = keyIdPattern.matcher(xmlString);

      // Build map of key names to identifiers
      java.util.Map<String, String> keyIdentifierMap = new java.util.HashMap<>();
      while (keyIdMatcher.find()) {
        String keyName = keyIdMatcher.group(1);
        String keyIdentifier = keyIdMatcher.group(2);
        keyIdentifierMap.put(keyName, keyIdentifier);
        System.out.println("Found keyIdentifier: " + keyName + " -> " + keyIdentifier);
      }

      while (matcher.find()) {
        String keyRef = matcher.group(1);
        String name = matcher.group(2);
        String privateKeyValue = matcher.group(3);

        Key key = new Key();
        key.setKeyRef(keyRef);
        key.setName(name);
        key.setPrivateKey(privateKeyValue);

        // Try to find keyIdentifier for this key
        String keyIdentifier = keyIdentifierMap.get(name);
        if (keyIdentifier != null) {
          key.setKeyIdentifier(keyIdentifier);
          System.out.println("Mapped keyIdentifier " + keyIdentifier + " to key: " + name);
        }

        keys.add(key);
        System.out.println("Extracted key: " + name);
      }
    } catch (Exception e) {
      System.err.println("Error extracting keys from XML: " + e.getMessage());
    }
  }
}
