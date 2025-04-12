package no.andreas.fugler;

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class FuglelyderSkraper {
    protected static final String WEBSITE = "https://www.fuglelyder.net/";
    protected static final Path BIRD_INDEX_SAVE = Path.of("birds", "birdIndex.ser");
    protected static final Path SOUND_INDEX_SAVE = Path.of("birds", "soundIndex");
    protected static final Path IMAGE_INDEX_SAVE = Path.of("birds", "imageIndex");
    protected static final Path SOUND_SAVES = Path.of("birds", "sounds");
    protected static final Path IMAGE_SAVES = Path.of("birds", "images");

    // API
    
    public static List<Bird> getBirdIndex() throws IOException, ClassNotFoundException {
      // Ready a birdIndex
      final List<Bird> birdIndex;
      if (Files.exists(BIRD_INDEX_SAVE)) {
        birdIndex = readBirdIndex(BIRD_INDEX_SAVE);
      } else {
        birdIndex = new ArrayList<>();
        downloadBirdIndex();
        birdIndex.addAll(readBirdIndex(BIRD_INDEX_SAVE));
        writeBirdIndex(BIRD_INDEX_SAVE, birdIndex);
      }
      return birdIndex;
    }
    
    public static HashMap<Bird, List<BirdSound>> getSoundIndex(final List<Bird> birdIndex) throws IOException, ClassNotFoundException {
      // Ready a soundIndex
      final HashMap<Bird, List<BirdSound>> soundIndex;
      if (Files.exists(SOUND_INDEX_SAVE)) {
        soundIndex = readSoundIndex(birdIndex, SOUND_INDEX_SAVE);
      } else {
        Files.createDirectories(SOUND_INDEX_SAVE);
        downloadSoundIndex(Objects.requireNonNull(birdIndex));
        soundIndex = readSoundIndex(birdIndex, BIRD_INDEX_SAVE);
        for (Bird bird : birdIndex) {
          Path soundsSave = SOUND_INDEX_SAVE.resolve(bird + ".ser");
          writeIndexSounds(soundsSave, soundIndex.get(bird));
        }
      }
      return soundIndex;
    }
      
    public static HashMap<Bird, List<BirdImage>> getImageIndex(final List<Bird> birdIndex) throws IOException, ClassNotFoundException {
      // Ready a soundIndex
      final HashMap<Bird, List<BirdImage>> imageIndex;
      if (Files.exists(IMAGE_INDEX_SAVE)) {
        imageIndex = readImageIndex(birdIndex, IMAGE_INDEX_SAVE);
      } else {
        Files.createDirectories(IMAGE_INDEX_SAVE);
        downloadSoundIndex(Objects.requireNonNull(birdIndex));
        imageIndex = readImageIndex(birdIndex, BIRD_INDEX_SAVE);
        for (Bird bird : birdIndex) {
          Path imagesSave = IMAGE_INDEX_SAVE.resolve(bird + ".ser");
          writeIndexImages(imagesSave, imageIndex.get(bird));
        }
      }
      return imageIndex;
    }
    
    public static List<Bird> readBirdIndex(final Path birdIndexSave) throws IOException, ClassNotFoundException {
      final List<Bird> birdIndex = new ArrayList<>();
      try (
        InputStream stream = Files.newInputStream(birdIndexSave);
        ObjectInputStream in = new ObjectInputStream(stream);
      ) {
        Object object = in.readObject();
        while (Objects.nonNull(object)) {
          if (object instanceof Bird) {
            birdIndex.add((Bird) object);
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
   
    public static HashMap<Bird, List<BirdSound>> readSoundIndex(final List<Bird> birdIndex, 
    final Path birdSoundIndexSave) throws IOException, ClassNotFoundException {
      // TODO: Create sibling method with different parameter signature by the save instead of both the index and save
      // try (DirectoryStream<Path> stream = Files.newDirectoryStream(birdSoundIndexSave)) {
      //   for (Path entry: stream) {
      //       ...
      //   }
      // }
      HashMap<Bird, List<BirdSound>> birdSoundIndex = new HashMap<>();
      for (Bird bird : birdIndex) {
        Path soundsSave = birdSoundIndexSave.resolve(bird + ".ser");
        birdSoundIndex.put(bird, readSoundIndexEntry(soundsSave));
      }
      return birdSoundIndex;
    }
   
    public static List<BirdSound> readSoundIndexEntry(final Path soundsSave) throws IOException, ClassNotFoundException {
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
  
    public static HashMap<Bird, List<BirdImage>> readImageIndex(final List<Bird> birdIndex, 
    final Path birdSoundIndexSave) throws IOException, ClassNotFoundException {
      // TODO: Create sibling method with different parameter signature by the save instead of both the index and save
      // try (DirectoryStream<Path> stream = Files.newDirectoryStream(birdSoundIndexSave)) {
      //   for (Path entry: stream) {
      //       ...
      //   }
      // }
      HashMap<Bird, List<BirdImage>> birdImageIndex = new HashMap<>();
      for (Bird bird : birdIndex) {
        Path imagesSave = birdSoundIndexSave.resolve(bird + ".ser");
        birdImageIndex.put(bird, readImageIndexEntry(imagesSave));
      }
      return birdImageIndex;
    }
   
    public static List<BirdImage> readImageIndexEntry(final Path imagesSave) throws IOException, ClassNotFoundException {
      final List<BirdImage> images = new ArrayList<>();
      try (
        InputStream stream = Files.newInputStream(imagesSave);
        ObjectInputStream in = new ObjectInputStream(stream);
      ) {
        // Object should be a String object
        Object object = in.readObject();
        while (Objects.nonNull(object)) {
          if (object instanceof BirdImage) {
            images.add((BirdImage) object);
          } else {
            System.err.print("Unknown object found serialized in birdsound file (" + imagesSave + ")");
            System.err.println(object);
            System.err.println("The unknown object is ignored and not considered as a birdsound");
          }
          object = in.readObject();
        }
      } catch (EOFException e) {
        // All objects in file read
        return images;
      }
      return null;
    }
    
    public static void downloadBirdIndex() throws IOException {
      String reqString = WEBSITE + "alfabetisk/";
      List<Bird> birds = new ArrayList<>();
      Document doc = Jsoup.connect(reqString).get();
      Elements birdElements = doc.select(".bird-outer");
      Pattern birdSlugPattern = Pattern.compile("birdmedia\\/(\\d+)\\/.+");
      for (Element bird : birdElements) {
        final Integer id;
        final String slug;
        final String name;
        final Optional<Element> img = bird.getElementsByTag("img").stream().findFirst();
        if (img.isPresent()) {
          final Element birdImg = img.get();
          Matcher idSearch = birdSlugPattern.matcher(birdImg.attr("src").strip());
          idSearch.matches();
          id = Integer.valueOf(idSearch.group(1)); 
          name = birdImg.attr("alt");
        } else {
          System.err.println("image element could not be found");
          id = null;
          name = null;
        }
        final Optional<Element> birdLink = bird.getElementsByTag("a").stream().findFirst();
        if (birdLink.isPresent()) {
          final Element link = birdLink.get();
          slug = link.attr("href").replace("/", "");
        } else {
          System.err.print("link element (a) could not be found");
          slug = null;
        }
        // TODO: Review if should have this behaviour. Adds a bird with all null parameters if elements could not be found.
        birds.add(new Bird(id, slug, name));
      }
      writeBirdIndex(BIRD_INDEX_SAVE, birds);
    }
    
    public static void downloadSoundIndex(List<Bird> birdIndex) throws IOException {
      for (Bird bird : birdIndex) {
        downloadSoundIndexEntry(bird);
      }
    }
  
    public static void downloadSoundIndexEntry(Bird bird) throws IOException {
      Files.createDirectories(SOUND_INDEX_SAVE);
      String birdLink = WEBSITE + bird.getSlug();
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
    
    public static void downloadAllSounds(final HashMap<Bird, List<BirdSound>> soundIndex)
    throws IOException, InterruptedException {
      // Download bird sounds
      Files.createDirectories(SOUND_SAVES);
      HttpClient birdSoundClient = HttpClient.newHttpClient();
      for (Bird bird : soundIndex.keySet()) {
        List<BirdSound> sounds = soundIndex.get(bird);
        for (BirdSound sound : sounds) {
          // TODO: Make the download request concurrent
          Path birdSoundSave = SOUND_SAVES.resolve(bird.getSlug() + sound + ".mp3");
          if (!Files.exists(birdSoundSave)) {
            try {
              String birdSoundURI = WEBSITE + "birdmedia/" + bird.getId() + "/" + sound.getId() + ".mp3";
              System.out.println(birdSoundURI);
              HttpRequest birdSoundReq = HttpRequest.newBuilder(new URI(birdSoundURI)).build();
              birdSoundClient.send(birdSoundReq, HttpResponse.BodyHandlers.ofFile(birdSoundSave));
            } catch (URISyntaxException e) {
              System.err.println("was unable to download sounds due to the uri being wrong");
            }
          }
        }
      }
    }
     
    public static void downloadImageIndex(List<Bird> birdIndex) throws IOException, ClassNotFoundException {
      Files.createDirectories(IMAGE_INDEX_SAVE);
      for (Bird bird : birdIndex) {
        downloadImageIndexEntry(bird);
      }
    }   
  
    public static void downloadImageIndexEntry(Bird bird) throws IOException {
      // TODO: The request to the bird slug page is incorrect. Should be replaced by request to https://www.fuglelyder.net/fuglgallery.asp?ID={{Bird id}}
      Files.createDirectories(IMAGE_INDEX_SAVE);
      String birdLink = WEBSITE + "fuglgallery.asp?ID=" + bird.getId();
      List<BirdImage> images = new ArrayList<>();
      Document birdDoc = Jsoup.connect(birdLink).get();
      Path imagesSave = IMAGE_INDEX_SAVE.resolve(bird + ".ser");
      Elements slides = Objects.requireNonNull(birdDoc.getElementsByClass("swiper-slide"));
      for (Element slide : slides) {
        final String slug = slide.getElementsByTag("img").first().attr("src").replace("birdmedia/" + bird.getId() + "/", "");
        final String photographer = slide.getElementsByTag("a").first().text();
        images.add(new BirdImage(slug, photographer));
      }
      writeIndexImages(imagesSave, images);
    }
   
    public static void downloadAllImages(final HashMap<Bird, List<BirdImage>> imageIndex)
    throws IOException, InterruptedException {
      // Download bird sounds
      Files.createDirectories(IMAGE_SAVES);
      final HttpClient birdImageClient = HttpClient.newHttpClient();
      for (Bird bird : imageIndex.keySet()) {
        final List<BirdImage> images = imageIndex.get(bird);
        for (BirdImage image : images) {
          // TODO: Make the download request concurrent
          // TODO: Perform the right download request
          final Path birdImageSave = IMAGE_SAVES.resolve(bird.toString() + image.toString());
          if (!Files.exists(birdImageSave)) {
            try {
              final String birdImageURI = WEBSITE + "birdmedia/" + bird.getId() + "/" + image.getSlug();
              final HttpRequest birdImageReq = HttpRequest.newBuilder(new URI(birdImageURI)).build();
              birdImageClient.send(birdImageReq, HttpResponse.BodyHandlers.ofFile(birdImageSave));
            } catch (URISyntaxException e) {
              System.err.println("was unable to download sounds due to the uri being wrong");
            }
          }
        }
      }
    }
  
    public static void writeIndexSounds(final Path soundsSave, final List<BirdSound> birdSounds) throws IOException {
      try (
        OutputStream stream = Files.newOutputStream(soundsSave);
        ObjectOutputStream out = new ObjectOutputStream(stream);
      ) {
        for (BirdSound birdSound : birdSounds) {
          out.writeObject(birdSound);
        }
      }
    }
  
    public static void writeIndexImages(final Path imagesSave, final List<BirdImage> birdImages) throws IOException {
      try (
        OutputStream stream = Files.newOutputStream(imagesSave);
        ObjectOutputStream out = new ObjectOutputStream(stream);
      ) {
        for (BirdImage birdImage : birdImages) {
          out.writeObject(birdImage);
        }
      }
    }
  
    public static void writeBirdIndex(final Path birdIndexSave, final List<Bird> birdIndex) throws IOException {
      try (
        OutputStream stream = Files.newOutputStream(birdIndexSave);
        ObjectOutputStream out = new ObjectOutputStream(stream);
      ) {
        for (Bird bird : birdIndex) {
          out.writeObject(bird);
        }
      }
    }
  }
  