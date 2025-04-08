package no.andreas.fugler;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.util.*;
import java.net.URISyntaxException;
import java.lang.InterruptedException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FuglelyderSkraper {
  private static String WEBSITE = "https://www.fuglelyder.net/";



  private static List<String> downloadBirdIndex() throws IOException {
    String reqString = WEBSITE + "alfabetisk/";
    List<String> birds = new ArrayList<>();
    Document doc = Jsoup.connect(reqString).get();
    Elements birdElements = doc.select(".bird-outer div");
    for (Element bird : birdElements) {
      birds.addAll(bird.getElementsByTag("a").stream()
          .map(birdLink -> birdLink.attributes().get("href"))
          .map(birdName -> birdName.replace("/", ""))
          .distinct().toList());
    }
    return birds;
  }

  private static List<BirdSound> downloadSound(String bird) throws IOException {
    String birdLink = WEBSITE + bird;
    Document birdDoc = Jsoup.connect(birdLink).get();
    System.out.println(bird);
    return Objects.requireNonNull(birdDoc.getElementById("soundtypes"))
        .getElementsByTag("div").stream()
        .filter(tab -> !tab.hasClass("audioshop"))
        .filter(tab -> !tab.id().equalsIgnoreCase("soundtypes"))
        .map(tab -> new BirdSound(Integer.valueOf(
            tab.id().replace("type", "")),
            tab.text().replace(" / ", "-")))
        .toList();
  }

  private static HashMap<String, List<BirdSound>> downloadSoundIndex(List<String> birdIndex)
      throws IOException {
    HashMap<String, List<BirdSound>> birdSoundIndex = new HashMap<>();
    for (String bird : birdIndex) {
      birdSoundIndex.put(bird, downloadSound(bird));
      System.out.println(bird + ": " + birdSoundIndex.get(bird));
    }
    return birdSoundIndex;
  }

  public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
    Files.createDirectories(Path.of("birds"));

    final List<String> birdIndex = getBirdIndex();

    final HashMap<String, List<BirdSound>> soundIndex = getSoundIndex(birdIndex);

    downloadAllSounds(soundIndex);
  }

  private static List<String> getBirdIndex() throws IOException, ClassNotFoundException {
    // Ready a birdIndex
    final List<String> birdIndex;
    final Path birdIndexSave = Path.of("birds", "birdIndex.ser");
    if (Files.exists(birdIndexSave)) {
      birdIndex = readBirdIndex(birdIndexSave);
    } else {
      birdIndex = new ArrayList<>();
      birdIndex.addAll(downloadBirdIndex());
      writeBirdIndex(birdIndexSave, birdIndex);
    }
    return birdIndex;
  }

  private static HashMap<String, List<BirdSound>> getSoundIndex(final List<String> birdIndex)
      throws IOException, ClassNotFoundException {
    // Ready a soundIndex
    final HashMap<String, List<BirdSound>> soundIndex;
    final Path soundIndexSave = Path.of("birds", "soundIndex");
    if (Files.exists(soundIndexSave)) {
      soundIndex = readSoundIndex(birdIndex, soundIndexSave);
    } else {
      Files.createDirectories(soundIndexSave);
      soundIndex = downloadSoundIndex(Objects.requireNonNull(birdIndex));
      for (String bird : birdIndex) {
        Path soundsSave = soundIndexSave.resolve(bird + ".ser");
        writeIndexSounds(soundsSave, soundIndex.get(bird));
      }
    }
    return soundIndex;
  }

  private static void downloadAllSounds(final HashMap<String, List<BirdSound>> soundIndex)
      throws IOException, InterruptedException {
    // Download bird sounds
    final Path birdSoundSaves = Path.of("birds", "sounds");
    Files.createDirectories(birdSoundSaves);
    HttpClient birdSoundClient = HttpClient.newHttpClient();
    for (String bird : soundIndex.keySet()) {
      List<BirdSound> sounds = soundIndex.get(bird);
      for (BirdSound sound : sounds) {
        // TODO: Make the download request concurrent
        Path birdSoundSave = birdSoundSaves.resolve(bird + sound + ".mp3");
        if (!Files.exists(birdSoundSave)) {
          try {
            String birdSoundURI = WEBSITE + "sounds/" + sound.getId() + ".mp3";
            HttpRequest birdSoundReq = HttpRequest.newBuilder(new URI(birdSoundURI)).build();
            birdSoundClient.send(birdSoundReq, HttpResponse.BodyHandlers.ofFile(birdSoundSave));
          } catch (URISyntaxException e) {
            System.err.println("was unable to download sounds due to the uri being wrong");
          }
        }
      }
    }
  }

  private static HashMap<String, List<BirdSound>> readSoundIndex(final List<String> birdIndex, 
      final Path birdSoundIndexSave) throws IOException, ClassNotFoundException {
    // TODO: Create sibling method with different parameter signature by the save instead of both the index and save
    // try (DirectoryStream<Path> stream = Files.newDirectoryStream(birdSoundIndexSave)) {
    //   for (Path entry: stream) {
    //       ...
    //   }
    // }
    HashMap<String, List<BirdSound>> birdSoundIndex = new HashMap<>();
    for (String bird : birdIndex) {
      Path soundsSave = birdSoundIndexSave.resolve(bird + ".ser");
      if (Files.exists(soundsSave)) {
        birdSoundIndex.put(bird, readBirdSounds(soundsSave));
      } else {
        List<BirdSound> birdSounds = downloadSound(bird);
        birdSoundIndex.put(bird, birdSounds);
        writeIndexSounds(soundsSave, birdSounds);
      }
    }
    return birdSoundIndex;
  }

  private static void writeBirdIndex(final Path birdIndexSave, final List<String> birdIndex) throws IOException {
    try (
        OutputStream stream = Files.newOutputStream(birdIndexSave);
        ObjectOutputStream out = new ObjectOutputStream(stream);
    ) {
      for (String bird : birdIndex) {
        out.writeObject(bird);
      }
    }
  }

  private static List<String> readBirdIndex(final Path birdIndexSave) throws IOException, ClassNotFoundException {
    final List<String> birdIndex = new ArrayList<>();
    try (
        InputStream stream = Files.newInputStream(birdIndexSave);
        ObjectInputStream in = new ObjectInputStream(stream);
    ) {
      Object object = in.readObject();
      while (Objects.nonNull(object)) {
        if (object instanceof String) {
          birdIndex.add((String) object);
        } else {
          System.err.print("Unknown object found serialized in birdindex file (" + birdIndexSave + ")");
          System.err.println(object);
          System.err.println("The unknown object is ignored and not considered as a birdindex");
        }
        object = in.readObject();
      }
    } catch (EOFException e) {
      return birdIndex;
    }
    return null;
  }

  private static void writeIndexSounds(final Path soundsSave, final List<BirdSound> birdSounds) throws IOException {
    try (
        OutputStream stream = Files.newOutputStream(soundsSave);
        ObjectOutputStream out = new ObjectOutputStream(stream);
    ) {
      for (BirdSound birdSound : birdSounds) {
        out.writeObject(birdSound);
      }
    }
  }

  private static List<BirdSound> readBirdSounds(final Path soundsSave) throws IOException, ClassNotFoundException {
    final List<BirdSound> sounds = new ArrayList<>();
    try (
        InputStream stream = Files.newInputStream(soundsSave);
        ObjectInputStream in = new ObjectInputStream(stream);
    ) {
      // Object should be a BirdSound object
      Object object = in.readObject();
      while (Objects.nonNull(object)) {
        if (object instanceof BirdSound) {
          sounds.add((BirdSound) object);
        } else {
          System.err.print("Unknown object found serialized in birdsound file (" + soundsSave + ")");
          System.err.println(object);
          System.err.println("The unknown object is ignored and not considered as a birdsound");
        }
        object = in.readObject();
      }
    } catch (EOFException e) {
      // All objects in file read
      return sounds;
    }
    return null;
  }
}

class BirdSound implements Serializable {
  private Integer id;
  private String type;

  public BirdSound(Integer id, String type) {
    this.id = id;
    this.type = type;
  }

  public Integer getId() {
    return id;
  }

  public String getType() {
    return type;
  }

  public void setId(Integer id) {
    this.id = id;
  }

  public void setType(String type) {
    this.type = type;
  }

  @Override
  public String toString() {
    return this.id + " " + this.type;
  }
}
