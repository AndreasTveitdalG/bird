package no.andreas.fugler;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.lang.InterruptedException;
import java.nio.file.Files;
import java.nio.file.Path;

@Command(
  name = "fugl",
  subcommands = {
    ListCommand.class,
    DownloadCommand.class,
    DeleteCommand.class,
    UpdateCommand.class
  },
  mixinStandardHelpOptions = true
)
public class FuglelyderCLI extends FuglelyderSkraper {
  public static void main(String[] args) throws IOException, InterruptedException, ClassNotFoundException {
    Files.createDirectories(Path.of("birds"));
    int exitCode = new CommandLine(new FuglelyderCLI()).execute(args);
    System.exit(exitCode);
    // System.out.println("Select a command to use. Run without argument or subargument for help.");
  }
}

// CLI
@Command(name = "list")
class ListCommand extends FuglelyderSkraper {
  @Command(name = "birds")
  int birds() throws IOException, ClassNotFoundException {
    // List all birds by ID
    final List<Bird> birdIndex = getBirdIndex();
    for (Bird bird : birdIndex) {
      System.out.println(bird);
    }
    // TODO: Create a useful exit code
    return 0;
  }
  
  @Command(name = "all-sounds")
  int allSounds() throws IOException, ClassNotFoundException {
    // List all sounds by ID for every bird
    final List<Bird> birdIndex = getBirdIndex();
    final HashMap<Bird, List<BirdSound>> soundIndex = getSoundIndex(birdIndex);
    for (Bird bird : soundIndex.keySet()) {
      System.out.println(bird);
      for (BirdSound sound : soundIndex.get(bird)) {
        System.out.println("\t" + sound);
      }
    }
    // TODO: Create a useful exit code
    return 0;
  }
  
  @Command(name = "all-images")
  int allImages() throws IOException, ClassNotFoundException {
    // List all sounds by ID for every bird
    final List<Bird> birdIndex = getBirdIndex();
    final HashMap<Bird, List<BirdImage>> imageIndex = getImageIndex(birdIndex);
    for (Bird bird : imageIndex.keySet()) {
      System.out.println(bird);
      for (BirdImage sound : imageIndex.get(bird)) {
        System.out.println("\t" + sound);
      }
    }
    // TODO: Create a useful exit code
    return 0;
  }

  @Command(name = "sounds")
  int sounds(@Parameters() String[] birds) throws IOException, ClassNotFoundException {
    // List all sounds by ID for each bird
    for (String bird : birds) {
      final List<Bird> birdIndex = getBirdIndex();
      final HashMap<Bird, List<BirdSound>> soundIndex = getSoundIndex(birdIndex);
      // TODO: Create better method of checking of contains; will always return false as is
      if (soundIndex.containsKey(new Bird(null, bird, null))) {
        for (BirdSound sound : soundIndex.get(new Bird(null, bird, null))) {
          System.out.println(sound);
        }
      } else {
        System.out.println("bird `" + bird + "` was not found in the cache");
      }
    }
    // TODO: Create a useful exit code
    return 0;
  }

  @Command(name = "images")
  int images(@Parameters String[] birds) throws IOException, ClassNotFoundException {
    // List all sounds by ID for each bird
    for (String bird : birds) {
      final List<Bird> birdIndex = getBirdIndex();
      final HashMap<Bird, List<BirdImage>> imageIndex = getImageIndex(birdIndex);
      // TODO: Create better method of checking of contains; will always return false as is
      if (imageIndex.containsKey(new Bird(null, bird, null))) {
        for (BirdImage image : imageIndex.get(new Bird(null, bird, null))) {
          System.out.println(image);
        }
      } else {
        System.out.println("bird `" + bird + "` was not found in the cache");
      }
    }
    // TODO: Create a useful exit code
    return 0;
  }
  // default -> {
  //   System.out.println("List the pieces of data that are cached");
  //   System.out.println("Availible subcommands are `birds, all-sounds, sounds and images`");
  // }
}

@Command(name = "download")
class DownloadCommand extends FuglelyderSkraper implements Callable<Integer> {
  @Parameters(index = "0")
  String target;

  @Override
  public Integer call() throws IOException, InterruptedException, ClassNotFoundException {
    // TODO: enable the possibility of downloading specific sounds and images
    switch (target) {
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
    // TODO: Create a useful exit code
    return 0;
  }
}

@Command(name = "delete")
class DeleteCommand extends FuglelyderSkraper implements Callable<Integer> {
  @Parameters()
  String target;

  @Override
  public Integer call() throws IOException {
    // add deletion of induvidual sounds and images
    switch (target) {
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
    // TODO: Create a useful exit code
    return 0;
  }
}

@Command(name = "update")
class UpdateCommand extends FuglelyderSkraper implements Callable<Integer> {
  @Parameters()
  String target;

  @Override
  public Integer call() throws IOException, ClassNotFoundException {
    // create logic
    switch (target) {
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
    // TODO: Create a useful exit code
    return 0;
  }
}
