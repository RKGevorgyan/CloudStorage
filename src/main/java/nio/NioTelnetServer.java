package nio;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NioTelnetServer {
    private final ByteBuffer buffer = ByteBuffer.allocate(512);

    private String rootDirectory; // Для хранения пути
    private String directory; // Для хранения текущей папки
    private List<String> directories = new ArrayList<>(); // Для хранения иерархии папок

    public static final String LS_COMMAND = "\tls          view all files from current directory\n";
    public static final String MKDIR_COMMAND = "\tmkdir       create directory\n";
    public static final String TOUCH_COMMAND = "\ttouch       create file\n";
    public static final String EXIT_COMMAND = "\texit        close connection\n";
    public static final String CD_COMMAND = "\tcd          change directory\n";
    public static final String RM_COMMAND = "\trm          delete file or directory\n";
    public static final String COPY_COMMAND = "\tcopy to:    copy file\n";
    public static final String CAT_COMMAND = "\tcat         show file content\n";

    public NioTelnetServer() throws IOException {

        directories.add("server");
        rootDirectory = "server" + File.separator;
        directory = "server";

        ServerSocketChannel server = ServerSocketChannel.open(); // открыли
        server.bind(new InetSocketAddress(1234));
        server.configureBlocking(false); // ВАЖНО
        Selector selector = Selector.open();
        server.register(selector, SelectionKey.OP_ACCEPT);
        System.out.println("Server started");
        while (server.isOpen()) {
            selector.select();
            var selectionKeys = selector.selectedKeys();
            var iterator = selectionKeys.iterator();
            while (iterator.hasNext()) {
                var key = iterator.next();
                if (key.isAcceptable()) {
                    handleAccept(key, selector);
                } else if (key.isReadable()) {
                    handleRead(key, selector);
                }
                iterator.remove();
            }
        }
    }

    private void handleRead(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = (SocketChannel) key.channel();
        SocketAddress client = channel.getRemoteAddress();
        int readBytes = channel.read(buffer);
        if (readBytes < 0) {
            channel.close();
            return;
        } else if (readBytes == 0) {
            return;
        }

        buffer.flip();
        StringBuilder sb = new StringBuilder();
        while (buffer.hasRemaining()) {
            sb.append((char) buffer.get());
        }
        buffer.clear();

        // TODO: 05.03.2021
        // touch (имя файла) - создание файла +
        // mkdir (имя директории) - создание директории +
        // cd (path) - перемещение по дереву папок +
        // rm (имя файла или папки) - удаление объекта
        // copy (src, target) - копирование файла
        // cat (имя файла) - вывод в консоль содержимого

        if (key.isValid()) {
            String command = sb.toString()
                    .replace("\n", "")
                    .replace("\r", "");
            if ("--help".equals(command)) {
                sendMessage(EXIT_COMMAND, selector,client);
                sendMessage(LS_COMMAND, selector,client);
                sendMessage(MKDIR_COMMAND, selector,client);
                sendMessage(TOUCH_COMMAND, selector,client);
                sendMessage(CD_COMMAND, selector,client);
                sendMessage(RM_COMMAND, selector,client);
                sendMessage(COPY_COMMAND, selector,client);
                sendMessage(CAT_COMMAND, selector,client);
            } else if ("ls".equals(command)) {
                sendMessage(getFilesList().concat("\n"), selector,client);
            } else if ("exit".equals(command)) {
                System.out.println("Client logged out. IP: " + channel.getRemoteAddress());
                channel.close();
                return;
            }else if (command.startsWith("touch")){
                if (touch(command))
                    sendMessage("File created\n", selector,client);
                else {
                    sendMessage("File already exists\n", selector,client);
                }
            }else if (command.startsWith("mkdir")){
                if (mkdir(command))
                    sendMessage("Directory created\n", selector,client);
                else {
                    sendMessage("Directory already exists\n", selector,client);
                }
            }else if (command.startsWith("cd")){
                cd(command);
            }else if (command.startsWith("rm")){
                if (rm(command))
                    sendMessage("File or directory removed\n",selector,client);
                else sendMessage("Unable to delete file or directory from path: " + rootDirectory,selector,client);
            }else if (command.startsWith("copy")){
                if(copy(command))
                    sendMessage("File copied\n",selector,client);
                else sendMessage("Incorrect path\n",selector,client);
            }
            else if(command.startsWith("cat")){
                cat(command,selector,client);
            }
        }
        sendName(channel);
    }

    /**
     * Shows content of selected file
     * @param command
     * @param selector
     * @param client
     * @throws IOException
     */
    private void cat(String command, Selector selector, SocketAddress client) throws IOException {
        Pattern pattern = Pattern.compile("\\s.*");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            Path path = Path.of(rootDirectory + matcher.group().replace(" ", ""));
            if (Files.exists(path)) {
                Files.newBufferedReader(path)
                        .lines()
                        .forEach(line -> {
                            try {
                                sendMessage(line + "\n", selector, client);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });
            }
        }
    }

    /**
     * Creating copy of selected file to given directory and name
     * @param command
     * @throws IOException
     */
    private boolean copy(String command) {
        // ищем имя файла в команде
        Pattern pattern = Pattern.compile("\\s.+\\.\\w+\\s");
        Matcher matcher = pattern.matcher(command);
        String path1 = "";
        String fileName="";
        if (matcher.find()) {
            fileName = matcher.group().replace(" ","");
            path1 = rootDirectory + fileName;
        }
        // Ищем путь куда копировать
        pattern = Pattern.compile("to:.*");
        matcher = pattern.matcher(command);
        String path2 = "";
        if (matcher.find()){
            path2 = matcher.group().replace(" ","").replace("to:","");
            System.out.println(path2 + " matcher");
        }else {
            List<String> list = new ArrayList<>(Arrays.asList(command.split(" ")));
            path2 = rootDirectory + list.get(list.size()-1).replace(" ","");
            System.out.println(path2 + " list");
        }
        // Проверяем есть ли в директории указанный файл
        if (!Files.exists(Path.of(path2)) && !(path2.contains("."))) {
            System.out.println("Incorrect path2");
            return false;
        }
        if (Files.exists(Path.of(path1))) {
            if (path2.contains(".")) {
                try {
                    Files.copy(Path.of(path1), Path.of(path2), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (IOException e) {
                    System.out.println("Incorrect path to copy");
                    return false;
                }
            }else {
                try {
                    Files.copy(Path.of(path1), Path.of(path2 + File.separator + fileName), StandardCopyOption.REPLACE_EXISTING);
                    return true;
                } catch (IOException e) {
                    System.out.println("Incorrect path to copy");
                    return false;
                }
            }
        }
        return false;
    }

    /**
     * Creates file to selected path
     * @param command
     * @return True if file created
     * @throws IOException
     */
    private boolean touch(String command) throws IOException {
        Pattern pattern = Pattern.compile("\\s.*");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group().replace(" ","");
            if (!Files.exists(Path.of(rootDirectory+ File.separator + name))) {
                Files.createFile(Path.of(rootDirectory + File.separator + name));
                return true;
            }
        }
        return false;
    }

    /**
     * Creates directorry to selected path
     * @param command
     * @return True if directory created
     * @throws IOException
     */
    private boolean mkdir(String command) throws IOException {
        Pattern pattern = Pattern.compile("\\s.*");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group().replace(" ","");
            if (!Files.exists(Path.of(rootDirectory + File.separator + name))){
                Files.createDirectories(Path.of(rootDirectory + File.separator + name));
                return true;
            }
        }
        return false;
    }

    /**
     * Gives ability to change directory
     * @param command
     */
    private void cd(String command){
        // Для возврата на ступень выше
        if (command.equals("cd ..")){
            // Ограничиваем перемещение вверх до главной папки "server"
            if (rootDirectory.equals("server"+File.separator) || rootDirectory.equals("server"))
                return;
            // Удаляем из иерархии последнюю папку
            directories.remove(directory);
            StringBuilder sb = new StringBuilder();
            // Присваиваем новые значения для текущей и полной директорий
            for (String str : directories) {
                sb.append(str).append(File.separator);
                rootDirectory = String.valueOf(sb);
            }
            directory = directories.get(directories.size()-1);
            return;
        }
        // Для перемещения по директориям
        Pattern pattern = Pattern.compile("\\s.*");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            directory = matcher.group().replace(" ", "");
            if (Files.exists(Path.of(rootDirectory + directory))) {
                directories.add(directory);
                StringBuilder sb = new StringBuilder();
                for (String str : directories) {
                    sb.append(str).append(File.separator);
                    rootDirectory = String.valueOf(sb);
                }
            } else {
                System.out.println("No such directory");
                directory = directories.get(directories.size()-1);
            }
        }
    }

    /**
     * Removes selected file or directory
     * @param command
     * @return True if file or directory removed
     * @throws IOException
     */
    private boolean rm(String command) throws IOException {
        Pattern pattern = Pattern.compile("\\s.*");
        Matcher matcher = pattern.matcher(command);
        if (matcher.find()) {
            String name = matcher.group().replace(" ", "");
            Files.delete(Path.of(rootDirectory + File.separator + name));
            return true;
        }
        return false;
    }

    private void sendName(SocketChannel channel) throws IOException {
        channel.write(
                ByteBuffer.wrap(channel
                        .getRemoteAddress().toString()
                        .concat(":~")
                        .concat(String.valueOf(directory))
                        .concat("~:$$$: ")
                        .getBytes(StandardCharsets.UTF_8)
                )
        );
    }

    private String getFilesList() {
        return String.join("\t", new File(String.valueOf(rootDirectory)).list());
    }

    private void sendMessage(String message, Selector selector, SocketAddress client) throws IOException {
        for (SelectionKey key : selector.keys()) {
            if (key.isValid() && key.channel() instanceof SocketChannel) {
                if (((SocketChannel) key.channel()).getRemoteAddress().equals(client)) {
                    ((SocketChannel) key.channel())
                            .write(ByteBuffer.wrap(message.getBytes(StandardCharsets.UTF_8)));
                }
            }
        }
    }

    private void handleAccept(SelectionKey key, Selector selector) throws IOException {
        SocketChannel channel = ((ServerSocketChannel) key.channel()).accept();
        channel.configureBlocking(false);
        System.out.println("Client accepted. IP: " + channel.getRemoteAddress());
        channel.register(selector, SelectionKey.OP_READ, "some attach");
        channel.write(ByteBuffer.wrap("Hello user!\n".getBytes(StandardCharsets.UTF_8)));
        channel.write(ByteBuffer.wrap("Enter --help for support info\n".getBytes(StandardCharsets.UTF_8)));
        sendName(channel);
    }

    public static void main(String[] args) throws IOException {
            new NioTelnetServer();
    }
}