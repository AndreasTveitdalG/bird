package no.andreas.fugler;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;
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
  private static final String WEBSITE = "https://www.fuglelyder.net/";
  private static final Path BIRD_INDEX_SAVE = Path.of("birds", "birdIndex.ser");
  private static final Path SOUND_INDEX_SAVE = Path.of("birds", "soundIndex");
  private static final Path IMAGE_INDEX_SAVE = Path.of("birds", "imageIndex");
  private static final Path SOUND_SAVES = Path.of("birds", "sounds");
  private static final Path IMAGE_SAVES = Path.of("birds", "images");
  
  public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
    Files.createDirectories(Path.of("birds"));
    
    if (args.length < 0) {
      System.out.println("Select a command to use. Run without argument or subargument for help.");
      return;
    }
    switch (args[0]) {
      case "list" -> listCommand(args);
      case "download" -> downloadCommand(args);
      case "delete" -> deleteCommand(args);
      case "update" -> updateCommand(args);
      default -> {
        System.out.println("Select a command to use. Run without argument or subargument for help.");
      }
    }
  }
  
  // CLI
  
  private static void listCommand(String[] args) throws IOException, ClassNotFoundException {
    if (args.length < 2) {
      System.out.println("List the pieces of data that are cached");
      System.out.println("Availible subcommands are `birds, all-sounds, sounds and images`");
      return;
    }
    switch (args[1]) {
      case "birds" -> {
        // List all birds by ID
        final List<String> birdIndex = getBirdIndex();
        for (String bird : birdIndex) {
          System.out.println(bird);
        }
      }
      case "all-sounds" -> {
        // List all sounds by ID for every bird
        final List<String> birdIndex = getBirdIndex();
        final HashMap<String, List<BirdSound>> soundIndex = getSoundIndex(birdIndex);
        for (String bird : soundIndex.keySet()) {
          System.out.println(bird);
          for (BirdSound sound : soundIndex.get(bird)) {
            System.out.println("\t" + sound);
          }
        }
      }
      case "sounds" -> {
        // TODO: extend to take multiple birds at once
        // List all sounds by ID for one particular bird
        if (args.length < 3) {
          System.out.println("List the pieces of data that are cached");
          System.out.println("Availible subcommands are `birds, all-sounds, sounds and images`");
          return;
        }
        final List<String> birdIndex = getBirdIndex();
        final HashMap<String, List<BirdSound>> soundIndex = getSoundIndex(birdIndex);
        if (soundIndex.containsKey(args[2])) {
          for (BirdSound sound : soundIndex.get(args[2])) {
            System.out.println(sound);
          }
        } else {
          System.out.println("bird `" + args[2] + "` was not found in the cache");
        }
      }
      case "images" -> {
        // TODO: Create image API
        System.err.println("Image API is still in development");
      }
      default -> {
        System.out.println("List the pieces of data that are cached");
        System.out.println("Availible subcommands are `birds, all-sounds, sounds and images`");
      }
    }
  }
  
  private static void downloadCommand(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
    // TODO: enable the possibility of downloading specific sounds and images
    if (args.length < 2) {
      System.out.println("Download sounds or images to the cache");
      System.out.println("Can be done in batch `all-images and all-sounds` or individually for a bird or a particular image/sound");
      return;
    }
    switch (args[1]) {
      case "all-sounds" -> {
        // Download all sounds for all birds
        final List<String> birdIndex = getBirdIndex();
        final HashMap<String, List<BirdSound>> soundIndex = getSoundIndex(birdIndex);
        downloadAllSounds(soundIndex);
      }
      case "all-images" -> {
        // TODO: Create image API
        System.err.println("Image API is still in development");
      }
      default -> {
        System.out.println("Download sounds or images to the cache");
        System.out.println("Can be done in batch `all-images and all-sounds` or individually for a bird or a particular image/sound");
      }
    }
  }
  
  private static void deleteCommand(String[] args) throws IOException {
    // add deletion of induvidual sounds and images
    if (args.length < 2) {
      System.out.println("Delete the saved content. Delete the entire save or specific content within it*");
      System.out.println("  *specific content deletion is not availible for the bird index");
      return;
    }
    switch (args[1]) {
      case "bird-index" -> {
        if (!Files.isDirectory(BIRD_INDEX_SAVE)) {
          System.out.println("Bird index should not be a directory");
        }
        if (Files.deleteIfExists(BIRD_INDEX_SAVE)) {
          System.out.println("Bird index has been successfully deleted");
        }
      }
      case "sound-index" -> {
        try (Stream<Path> files = Files.walk(SOUND_INDEX_SAVE)) {
          for (Path file : files.toList()) {
            Files.deleteIfExists(file);
          }
        }
        System.out.println("Sound index has been successfully deleted");
      }
      case "image-index" -> {
        try (Stream<Path> files = Files.walk(IMAGE_INDEX_SAVE)) {
          for (Path file : files.toList()) {
            Files.deleteIfExists(file);
          }
        }
      }
      case "sounds" -> {
        try (Stream<Path> files = Files.walk(SOUND_SAVES)) {
          for (Path file : files.toList()) {
            Files.deleteIfExists(file);
          }
        }
      }
      case "images" -> {
        try (Stream<Path> files = Files.walk(IMAGE_SAVES)) {
          for (Path file : files.toList()) {
            Files.deleteIfExists(file);
          }
        }
      }
      default -> {
        System.out.println("Delete the saved content. Delete the entire save or specific content within it*");
        System.out.println("  *specific content deletion is not availible for the bird index");
      }
    }
  }
  
  private static void updateCommand(String[] args) throws IOException, ClassNotFoundException {
    // create logic
    if (args.length < 2) {
      System.out.println("Update the indexes. Specify which to update. `alls-indexes, bird-index, sound-index or image-index`");
      return;
    }
    switch (args[1]) {
      case "all-indexes" -> {
        final List<String> birdIndex = downloadBirdIndex();
        downloadSoundIndex(birdIndex);
      }
      case "bird-index" -> {
        downloadBirdIndex();
      }
      case "sound-index" -> {
        final List<String> birdIndex = getBirdIndex();
        downloadSoundIndex(birdIndex);
      }
      case "image-index" -> {
        // TODO: Create image API
        System.err.println("Image API is still in development");
      }
      default -> {
        System.out.println("Update the indexes. Specify which to update. `alls-indexes, bird-index, sound-index or image-index`");
      }
    }
  }
  
  // API
  
  private static List<String> getBirdIndex() throws IOException, ClassNotFoundException {
    // Ready a birdIndex
    final List<String> birdIndex;
    if (Files.exists(BIRD_INDEX_SAVE)) {
      birdIndex = readBirdIndex(BIRD_INDEX_SAVE);
    } else {
      birdIndex = new ArrayList<>();
      birdIndex.addAll(downloadBirdIndex());
      writeBirdIndex(BIRD_INDEX_SAVE, birdIndex);
    }
    return birdIndex;
  }
  
  private static HashMap<String, List<BirdSound>> getSoundIndex(final List<String> birdIndex)
  throws IOException, ClassNotFoundException {
    // Ready a soundIndex
    final HashMap<String, List<BirdSound>> soundIndex;
    if (Files.exists(SOUND_INDEX_SAVE)) {
      soundIndex = readSoundIndex(birdIndex, SOUND_INDEX_SAVE);
    } else {
      Files.createDirectories(SOUND_INDEX_SAVE);
      downloadSoundIndex(Objects.requireNonNull(birdIndex));
      soundIndex = readSoundIndex(birdIndex, BIRD_INDEX_SAVE);
      for (String bird : birdIndex) {
        Path soundsSave = SOUND_INDEX_SAVE.resolve(bird + ".ser");
        writeIndexSounds(soundsSave, soundIndex.get(bird));
      }
    }
    return soundIndex;
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
      birdSoundIndex.put(bird, readSoundIndexEntry(soundsSave));
    }
    return birdSoundIndex;
  }
 
  private static List<BirdSound> readSoundIndexEntry(final Path soundsSave) throws IOException, ClassNotFoundException {
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
  
  private static void downloadSoundIndex(List<String> birdIndex) throws IOException {
    for (String bird : birdIndex) {
      downloadSoundIndexEntry(bird);
    }
  }

  private static void downloadSoundIndexEntry(String bird) throws IOException {
    String birdLink = WEBSITE + bird;
    Document birdDoc = Jsoup.connect(birdLink).get();
    Path soundsSave = SOUND_INDEX_SAVE.resolve(bird + ".ser");
    writeIndexSounds(soundsSave, Objects.requireNonNull(birdDoc.getElementById("birdmedia"))
      .getElementsByClass("allsounds").first()
      .getElementsByTag("span").stream()
      .filter(tab -> !tab.hasClass("heartspan"))
      .filter(tab -> !tab.id().equalsIgnoreCase("soundshop"))
      .map(tab -> new BirdSound(Integer.valueOf(
          tab.id().replace("sound", "")),
        tab.text().replace(" / ", "-")))
      .toList());
  }
  
  private static void downloadAllSounds(final HashMap<String, List<BirdSound>> soundIndex)
  throws IOException, InterruptedException {
    // Download bird sounds
    Files.createDirectories(SOUND_SAVES);
    HttpClient birdSoundClient = HttpClient.newHttpClient();
    for (String bird : soundIndex.keySet()) {
      List<BirdSound> sounds = soundIndex.get(bird);
      for (BirdSound sound : sounds) {
        // TODO: Make the download request concurrent
        Path birdSoundSave = SOUND_SAVES.resolve(bird + sound + ".mp3");
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
}
