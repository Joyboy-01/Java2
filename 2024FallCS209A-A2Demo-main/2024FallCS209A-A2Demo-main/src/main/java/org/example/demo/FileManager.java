package org.example.demo;

import java.io.File;
import java.io.IOException;

public class FileManager {
    private static final String DATA_DIR = "data";  // 数据存放目录
    private static final String USERS_FILE = "2024FallCS209A-A2Demo-main/2024FallCS209A-A2Demo-main/data/users.txt";
    private static final String HISTORY_FILE = "2024FallCS209A-A2Demo-main/2024FallCS209A-A2Demo-main/data/game_history.txt";

    public static void initialize() {
        createFile(DATA_DIR);
        createFile(USERS_FILE);
        createFile(HISTORY_FILE);
    }

    private static void createFile(String path) {
        File file = new File(path);
        try {
            if (path.endsWith(".txt")) {
                if (!file.exists()) {
                    file.createNewFile();
                }
            } else {
                if (!file.exists()) {
                    file.mkdir();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String getUsersFilePath() {
        return USERS_FILE;
    }

    public static String getHistoryFilePath() {
        return HISTORY_FILE;
    }
}
