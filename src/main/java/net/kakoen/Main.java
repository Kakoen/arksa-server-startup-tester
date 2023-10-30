package net.kakoen;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;
import java.util.function.Predicate;

@Slf4j
public class Main {
    static final File SAVE_DEST = new File("D:\\SteamLibrary\\steamapps\\common\\ARK Survival Ascended Dedicated Server\\ShooterGame\\Saved\\SavedArks\\TheIsland_WP");
    static final File SAVE_SRC = new File("save_files");
    static final File SERVER_BINARY = new File("D:\\SteamLibrary\\steamapps\\common\\ARK Survival Ascended Dedicated Server\\ShooterGame\\Binaries\\Win64\\ArkAscendedServer.exe");
    static final String GOOD_TRIBES_FILE = "good_tribes.txt";
    static final String BAD_TRIBES_FILE = "bad_tribes.txt";
    static final int ASSUME_SUCCESSFUL_STARTUP_SECONDS = 120;

    public static void main(String[] args) throws IOException, InterruptedException {

        if(!SAVE_DEST.getName().equals("TheIsland_WP")) {
            log.error("Make sure the SAVE_DEST files points to the correct TheIsland_WP directory (ShooterGame\\Saved\\SavedArks\\TheIsland_WP in your server installation)!  This folder WILL be wiped!");
            return;
        }

        Set<String> goodTribes = readFromFile(GOOD_TRIBES_FILE);
        Set<String> badTribes = readFromFile(BAD_TRIBES_FILE);

        List<String> tribeFiles = Arrays.stream(Objects.requireNonNull(SAVE_SRC.listFiles()))
                .map(File::getName)
                .filter(name -> name.endsWith("arktribe"))
                .filter(name -> !goodTribes.contains(name))
                .filter(name -> !badTribes.contains(name))
                .toList();

        log.info("Assuming good tribes from earlier run: {}", goodTribes);
        log.info("Assuming bad tribes from earlier run: {}", badTribes);
        log.info("Other tribe files: {}", tribeFiles);

        Stack<List<String>> tribeFilesStack = new Stack<>();

        while(!tribeFiles.isEmpty()) {
            //Delete directory SAVE_DEST recursively
            if (!deleteDirectory(SAVE_DEST) && SAVE_DEST.exists()) {
                log.error("Failed to delete directory {}, trying again in 5 seconds...", SAVE_DEST);
                Thread.sleep(5000);
                continue;
            }

            if (!SAVE_DEST.mkdirs()) {
                log.error("Failed to create directory {}", SAVE_DEST);
                return;
            }

            final List<String> tribeFilesCopy = new ArrayList<>(tribeFiles);
            tribeFilesCopy.addAll(goodTribes);
            copyDirectory(SAVE_SRC, SAVE_DEST, (file) -> !file.getName().endsWith("arktribe") || tribeFilesCopy.contains(file.getName()));

            log.info("=====================================================");
            log.info("Running server with tribes {}", tribeFiles);
            log.info("Good tribes {}", goodTribes);
            log.info("Bad tribes {}", badTribes);
            log.info("Stack {}", tribeFilesStack.size());
            Process process = new ProcessBuilder(SERVER_BINARY.getAbsolutePath(), "TheIsland_WP?listen", "-UseBattlEye").directory(SERVER_BINARY.getParentFile()).start();
            Instant startDate = Instant.now();
            boolean saveWorks = false;
            while(process.isAlive()) {
                if (Instant.now().isAfter(startDate.plusSeconds(ASSUME_SUCCESSFUL_STARTUP_SECONDS))) {
                    saveWorks = true;
                    log.info("Process did not exit after {} seconds, succesfull test! Killing server...", ASSUME_SUCCESSFUL_STARTUP_SECONDS);
                    process.destroyForcibly();
                }
                Thread.sleep(1000);
            }
            log.info("Process exited with code {} after {} seconds", process.exitValue(), Instant.now().getEpochSecond() - startDate.getEpochSecond());

            if(!saveWorks) {
                if(tribeFiles.size() == 1) {
                    log.info("Bad tribe found and isolated: {}", tribeFiles);
                    badTribes.addAll(tribeFiles);
                    writeToFile(BAD_TRIBES_FILE, badTribes);

                    if(tribeFilesStack.isEmpty()) {
                        log.info("No more tribes to test!");
                        log.info("Bad tribes: {}", badTribes);
                        return;
                    }
                } else {
                    //remove half of the tribes and put them on the stack
                    int half = tribeFiles.size() / 2;
                    List<String> removedTribes = tribeFiles.subList(0, half);
                    tribeFiles = tribeFiles.subList(half, tribeFiles.size());
                    tribeFilesStack.push(removedTribes);
                    log.info("Put tribes on the stack for later testing {}", removedTribes);
                }
            } else {
                log.info("Save works!");
                log.info("Added good tribes: {}", tribeFiles);
                goodTribes.addAll(tribeFiles);
                writeToFile(GOOD_TRIBES_FILE, goodTribes);
                if(!tribeFilesStack.isEmpty()) {
                    tribeFiles = new ArrayList<>(tribeFilesStack.pop());
                } else {
                    log.info("Save works, no more tribes to test!");
                    log.info("Bad tribes: {}", badTribes);
                    return;
                }
            }
        }



    }

    private static void writeToFile(String fileName, Set<String> goodTribes) {
        try {
            Files.writeString(new File(fileName).toPath(), String.join("\n", goodTribes));
        } catch (IOException e) {
            log.error("Failed to write to file {}", fileName, e);
        }
    }

    private static Set<String> readFromFile(String fileName) {
        if(!new File(fileName).exists()) {
            return new HashSet<>();
        }
        try {
            return new HashSet<>(Files.readAllLines(new File(fileName).toPath()));
        } catch (IOException e) {
            log.error("Failed to read from file {}", fileName, e);
            return new HashSet<>();
        }
    }

    private static boolean copyDirectory(File saveSrc, File saveDest, Predicate<File> fileFilter) throws IOException {
        if (saveSrc.isDirectory()) {
            File[] files = saveSrc.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (!copyDirectory(file, saveDest, fileFilter)) {
                        return false;
                    }
                }
            }
        } else {
            if (fileFilter.test(saveSrc)) {
                File destFile = new File(saveDest, saveSrc.getName());
                destFile.getParentFile().mkdirs();
                Files.copy(saveSrc.toPath(), destFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
        return true;
    }

    private static boolean deleteDirectory(File saveDest) {
        if (saveDest.isDirectory()) {
            File[] files = saveDest.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        return saveDest.delete();
    }


}