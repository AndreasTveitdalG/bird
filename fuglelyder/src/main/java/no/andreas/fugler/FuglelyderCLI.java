package no.andreas.fugler;

import java.io.*;
import java.util.*;
import java.util.stream.Stream;
import java.lang.InterruptedException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FuglelyderCLI extends FuglelyderSkraper {
  
  public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
    Files.createDirectories(Path.of("birds"));
    
    if (args.length < 1) {
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
        final List<Bird> birdIndex = getBirdIndex();
        for (Bird bird : birdIndex) {
          System.out.println(bird);
        }
      }
      case "all-sounds" -> {
        // List all sounds by ID for every bird
        final List<Bird> birdIndex = getBirdIndex();
        final HashMap<Bird, List<BirdSound>> soundIndex = getSoundIndex(birdIndex);
        for (Bird bird : soundIndex.keySet()) {
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
        final List<Bird> birdIndex = getBirdIndex();
        final HashMap<Bird, List<BirdSound>> soundIndex = getSoundIndex(birdIndex);
        // TODO: Create better method of checking of contains; will always return false as is
        if (soundIndex.containsKey(new Bird(null, args[2], null))) {
          for (BirdSound sound : soundIndex.get(new Bird(null, args[2], null))) {
            System.out.println(sound);
          }
        } else {
          System.out.println("bird `" + args[2] + "` was not found in the cache");
        }
      }
      case "all-images" -> {
        // List all sounds by ID for every bird
        final List<Bird> birdIndex = getBirdIndex();
        final HashMap<Bird, List<BirdImage>> imageIndex = getImageIndex(birdIndex);
        for (Bird bird : imageIndex.keySet()) {
          System.out.println(bird);
          for (BirdImage sound : imageIndex.get(bird)) {
            System.out.println("\t" + sound);
          }
        }
      }
      case "images" -> {
        // TODO: extend to take multiple birds at once
        // List all sounds by ID for one particular bird
        if (args.length < 3) {
          System.out.println("List the pieces of data that are cached");
          System.out.println("Availible subcommands are `birds, all-sounds, sounds and images`");
          return;
        }
        final List<Bird> birdIndex = getBirdIndex();
        final HashMap<Bird, List<BirdImage>> imageIndex = getImageIndex(birdIndex);
        // TODO: Create better method of checking of contains; will always return false as is
        if (imageIndex.containsKey(new Bird(null, args[2], null))) {
          for (BirdImage image : imageIndex.get(new Bird(null, args[2], null))) {
            System.out.println(image);
          }
        } else {
          System.out.println("bird `" + args[2] + "` was not found in the cache");
        }
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
        final List<Bird> birdIndex = getBirdIndex();
        final HashMap<Bird, List<BirdSound>> soundIndex = getSoundIndex(birdIndex);
        downloadAllSounds(soundIndex);
      }
      case "all-images" -> {
        // Download all images for all birds
        final List<Bird> birdIndex = getBirdIndex();
        final HashMap<Bird, List<BirdImage>> imageIndex = getImageIndex(birdIndex);
        downloadAllImages(imageIndex);
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
        downloadBirdIndex();
        final List<Bird> birdIndex = readBirdIndex(BIRD_INDEX_SAVE);
        downloadSoundIndex(birdIndex);
        downloadImageIndex(birdIndex);
      }
      case "bird-index" -> {
        downloadBirdIndex();
      }
      case "sound-index" -> {
        final List<Bird> birdIndex = getBirdIndex();
        downloadSoundIndex(birdIndex);
      }
      case "image-index" -> {
        final List<Bird> birdIndex = getBirdIndex();
        downloadImageIndex(birdIndex);
      }
      default -> {
        System.out.println("Update the indexes. Specify which to update. `alls-indexes, bird-index, sound-index or image-index`");
      }
    }
  }
}
