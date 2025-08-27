import java.io.; import java.util.;

/**

Virtual File Management System (single-file Java program)


---

This is a teaching-oriented, in-memory hierarchical filesystem with

persistence to a single disk image (a .vfs file). It supports common

file/directory commands via an interactive REPL.

Key ideas (aligned with OS coursework):

Directory tree with a Root node ("/")


Path resolution (absolute/relative)


Basic metadata (ctime, mtime, size, type)


Simple permissions model (rwx --- simplified, no users)


Persistence via Java serialization (simulates mounting/unmounting)


Common commands: ls, tree, mkdir, touch, write, append, cat, cp, mv, rm, stat, find, pwd, cd, help, exit


How to run:

javac VirtualFileManager.java

java VirtualFileManager disk.vfs     # creates or loads disk image

Example session:

> mkdir /docs



> write /docs/readme.txt "Hello, OS world!"



> ls /docs



> cat /docs/readme.txt



> tree /



> save



> exit */ public class VirtualFileManager {




// ===== VNode: a file or directory in our virtual FS =====
private static class VNode implements Serializable {
    String name;
    boolean directory;
    byte[] data; // for files
    Map<String, VNode> children; // for directories
    long ctime, mtime; // creation & modification times (epoch millis)
    int perms; // rwx as bitmask: r=4, w=2, x=1 for owner; we keep a single triplet for simplicity

    VNode(String name, boolean directory) {
        this.name = name;
        this.directory = directory;
        this.data = directory ? null : new byte[0];
        this.children = directory ? new LinkedHashMap<>() : null;
        long now = System.currentTimeMillis();
        this.ctime = now;
        this.mtime = now;
        this.perms = 0b111; // default rwx for everyone (simplified)
    }

    int size() {
        if (directory) return children.size();
        return data == null ? 0 : data.length;
    }

    VNode deepCopy() {
        VNode copy = new VNode(this.name, this.directory);
        copy.ctime = this.ctime;
        copy.mtime = this.mtime;
        copy.perms = this.perms;
        if (this.directory) {
            for (Map.Entry<String, VNode> e : this.children.entrySet()) {
                copy.children.put(e.getKey(), e.getValue().deepCopy());
            }
        } else {
            copy.data = Arrays.copyOf(this.data, this.data.length);
        }
        return copy;
    }
}

// ===== FileSystem state persisted to disk image =====
private static class FileSystem implements Serializable {
    VNode root = new VNode("/", true);
    // Track current working directory path (e.g., "/", "/docs")
    String cwd = "/";
}

// ===== Runtime (not serialized) =====
private FileSystem fs;
private final File backingFile;

public VirtualFileManager(String imagePath) {
    this.backingFile = new File(imagePath);
    if (backingFile.exists()) {
        try (ObjectInputStream in = new ObjectInputStream(new BufferedInputStream(new FileInputStream(backingFile)))) {
            Object obj = in.readObject();
            this.fs = (FileSystem) obj;
            System.out.println("[mounted] Loaded disk image: " + imagePath);
        } catch (Exception e) {
            System.out.println("[warn] Could not load disk image. Formatting new FS: " + e.getMessage());
            this.fs = new FileSystem();
            save();
        }
    } else {
        this.fs = new FileSystem();
        save();
        System.out.println("[formatted] New disk image created: " + imagePath);
    }
}

// ===== Persistence =====
private void save() {
    try (ObjectOutputStream out = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(backingFile)))) {
        out.writeObject(fs);
        out.flush();
    } catch (IOException e) {
        System.out.println("[error] Save failed: " + e.getMessage());
    }
}

// ===== Path utilities =====
private static String normalize(String path, String cwd) {
    if (path == null || path.isEmpty()) return cwd;
    boolean abs = path.startsWith("/");
    Deque<String> stack = new ArrayDeque<>();
    if (abs) {
        // start from root
    } else {
        for (String p : cwd.split("/")) {
            if (!p.isEmpty()) stack.addLast(p);
        }
    }
    for (String part : path.split("/")) {
        if (part.isEmpty() || part.equals(".")) continue;
        if (part.equals("..")) {
            if (!stack.isEmpty()) stack.removeLast();
        } else {
            stack.addLast(part);
        }
    }
    StringBuilder sb = new StringBuilder("/");
    Iterator<String> it = stack.iterator();
    while (it.hasNext()) {
        sb.append(it.next());
        if (it.hasNext()) sb.append('/');
    }
    return sb.toString();
}

private static class ResolvedPath {
    VNode parent; // parent directory node (null if root itself)
    VNode node;   // target node (file or directory)
    String name;  // final path name
    String abs;   // absolute normalized path
}

private ResolvedPath resolve(String rawPath, boolean createParents, boolean wantParent) {
    String abs = normalize(rawPath, fs.cwd);
    if (abs.equals("/")) {
        ResolvedPath rp = new ResolvedPath();
        rp.parent = null; rp.node = fs.root; rp.name = "/"; rp.abs = "/";
        return rp;
    }
    String[] parts = Arrays.stream(abs.split("/")).filter(s -> !s.isEmpty()).toArray(String[]::new);
    VNode cur = fs.root; VNode parent = null; String name = "/";
    for (int i = 0; i < parts.length; i++) {
        name = parts[i];
        parent = cur;
        VNode next = parent.children.get(name);
        if (next == null) {
            if (createParents && i < parts.length - 1) {
                // auto-create intermediate dirs
                next = new VNode(name, true);
                parent.children.put(name, next);
            } else if (wantParent && i == parts.length - 1) {
                // want only the parent dir
                ResolvedPath rp = new ResolvedPath();
                rp.parent = parent; rp.node = null; rp.name = name; rp.abs = abs;
                return rp;
            } else {
                return null; // not found
            }
        }
        cur = next;
        if (!cur.directory && i < parts.length - 1) return null; // descending into a file
    }
    ResolvedPath rp = new ResolvedPath();
    rp.parent = parent; rp.node = cur; rp.name = name; rp.abs = abs;
    return rp;
}

// ===== Command implementations =====

private void cmd_pwd() {
    System.out.println(fs.cwd);
}

private void cmd_cd(String path) {
    ResolvedPath rp = resolve(path, false, false);
    if (rp == null || rp.node == null || !rp.node.directory) {
        System.out.println("cd: not a directory: " + path);
        return;
    }
    fs.cwd = rp.abs;
    System.out.println("cwd -> " + fs.cwd);
}

private void cmd_ls(String path) {
    ResolvedPath rp = (path == null) ? resolve(fs.cwd, false, false) : resolve(path, false, false);
    if (rp == null || rp.node == null) {
        System.out.println("ls: not found: " + (path == null ? fs.cwd : path));
        return;
    }
    VNode node = rp.node;
    if (!node.directory) {
        System.out.printf("%s\tFILE\t%d bytes\n", rp.abs, node.size());
        return;
    }
    for (Map.Entry<String, VNode> e : node.children.entrySet()) {
        VNode c = e.getValue();
        String type = c.directory ? "DIR " : "FILE";
        System.out.printf("%s\t%s\t%d\n", e.getKey(), type, c.size());
    }
}

private void cmd_tree(String path) {
    ResolvedPath rp = (path == null) ? resolve(fs.cwd, false, false) : resolve(path, false, false);
    if (rp == null || rp.node == null) { System.out.println("tree: not found"); return; }
    printTree(rp.node, rp.abs, "");
}

private void printTree(VNode node, String path, String indent) {
    System.out.println(indent + (indent.isEmpty() ? path : node.name) + (node.directory ? "/" : ""));
    if (node.directory) {
        for (VNode c : node.children.values()) {
            printTree(c, path + (path.equals("/") ? "" : "/") + c.name, indent + "  ");
        }
    }
}

private void cmd_mkdir(String path) {
    ResolvedPath rp = resolve(path, true, true);
    if (rp == null || rp.parent == null) { System.out.println("mkdir: invalid path"); return; }
    if (rp.parent.children.containsKey(rp.name)) {
        System.out.println("mkdir: already exists: " + rp.abs);
        return;
    }
    rp.parent.children.put(rp.name, new VNode(rp.name, true));
    save();
    System.out.println("created dir: " + rp.abs);
}

private void cmd_touch(String path) {
    ResolvedPath rp = resolve(path, true, true);
    if (rp == null || rp.parent == null) { System.out.println("touch: invalid path"); return; }
    VNode n = rp.parent.children.get(rp.name);
    if (n == null) {
        n = new VNode(rp.name, false);
        rp.parent.children.put(rp.name, n);
    }
    n.mtime = System.currentTimeMillis();
    save();
    System.out.println("touched: " + rp.abs);
}

private void cmd_write(String path, String content, boolean append) {
    ResolvedPath rp = resolve(path, true, true);
    if (rp == null || rp.parent == null) { System.out.println("write: invalid path"); return; }
    VNode n = rp.parent.children.get(rp.name);
    if (n == null) { n = new VNode(rp.name, false); rp.parent.children.put(rp.name, n); }
    if (n.directory) { System.out.println("write: path is a directory"); return; }
    byte[] bytes = content.getBytes();
    if (append && n.data != null) {
        byte[] merged = new byte[n.data.length + bytes.length];
        System.arraycopy(n.data, 0, merged, 0, n.data.length);
        System.arraycopy(bytes, 0, merged, n.data.length, bytes.length);
        n.data = merged;
    } else {
        n.data = bytes;
    }
    n.mtime = System.currentTimeMillis();
    save();
    System.out.println((append ? "appended to: " : "wrote: ") + rp.abs + " (" + n.size() + " bytes)");
}

private void cmd_cat(String path) {
    ResolvedPath rp = resolve(path, false, false);
    if (rp == null || rp.node == null) { System.out.println("cat: not found"); return; }
    if (rp.node.directory) { System.out.println("cat: is a directory"); return; }
    System.out.println(new String(rp.node.data));
}

private void cmd_rm(String path, boolean recursive) {
    ResolvedPath rp = resolve(path, false, false);
    if (rp == null || rp.node == null) { System.out.println("rm: not found"); return; }
    if (rp.node.directory && !recursive && !rp.node.children.isEmpty()) {
        System.out.println("rm: directory not empty (use rm -r)");
        return;
    }
    if (rp.parent == null) { System.out.println("rm: cannot remove root"); return; }
    rp.parent.children.remove(rp.name);
    save();
    System.out.println("removed: " + rp.abs);
}

private void cmd_mv(String src, String dst) {
    ResolvedPath rs = resolve(src, false, false);
    if (rs == null || rs.node == null) { System.out.println("mv: source not found"); return; }
    ResolvedPath rd = resolve(dst, true, true);
    if (rd == null || rd.parent == null) { System.out.println("mv: invalid destination"); return; }
    // moving into an existing directory keeps original name
    if (rd.parent.children.containsKey(rd.name) && rd.parent.children.get(rd.name).directory) {
        rd.parent.children.get(rd.name).children.put(rs.name, rs.node);
        if (rs.parent != null) rs.parent.children.remove(rs.name);
        save();
        System.out.println("moved into dir: " + normalize(dst + (dst.endsWith("/") ? "" : "/") + rs.name, fs.cwd));
        return;
    }
    // rename/move
    if (rs.parent == null) { System.out.println("mv: cannot move root"); return; }
    rs.parent.children.remove(rs.name);
    rs.node.name = rd.name;
    rd.parent.children.put(rd.name, rs.node);
    save();
    System.out.println("moved: " + rs.abs + " -> " + rd.abs);
}

private void cmd_cp(String src, String dst) {
    ResolvedPath rs = resolve(src, false, false);
    if (rs == null || rs.node == null) { System.out.println("cp: source not found"); return; }
    ResolvedPath rd = resolve(dst, true, true);
    if (rd == null || rd.parent == null) { System.out.println("cp: invalid destination"); return; }
    VNode copy = rs.node.deepCopy();
    copy.name = rd.name;
    // If destination is an existing directory, drop inside with original name
    if (rd.parent.children.containsKey(rd.name) && rd.parent.children.get(rd.name).directory) {
        copy.name = rs.name;
        rd.parent.children.get(rd.name).children.put(copy.name, copy);
        save();
        System.out.println("copied into dir: " + normalize(dst + (dst.endsWith("/") ? "" : "/") + copy.name, fs.cwd));
        return;
    }
    rd.parent.children.put(rd.name, copy);
    save();
    System.out.println("copied: " + rs.abs + " -> " + rd.abs);
}

private void cmd_stat(String path) {
    ResolvedPath rp = resolve(path, false, false);
    if (rp == null || rp.node == null) { System.out.println("stat: not found"); return; }
    VNode n = rp.node;
    System.out.println("path: " + rp.abs);
    System.out.println("type: " + (n.directory ? "directory" : "file"));
    System.out.println("size: " + n.size() + (n.directory ? " entries" : " bytes"));
    System.out.println("ctime: " + new Date(n.ctime));
    System.out.println("mtime: " + new Date(n.mtime));
    System.out.println("perms: " + permsToString(n.perms));
}

private String permsToString(int p) {
    char r = ((p & 0b100) != 0) ? 'r' : '-';
    char w = ((p & 0b010) != 0) ? 'w' : '-';
    char x = ((p & 0b001) != 0) ? 'x' : '-';
    return "" + r + w + x;
}

private void cmd_chmod(String path, String mode) {
    ResolvedPath rp = resolve(path, false, false);
    if (rp == null || rp.node == null) { System.out.println("chmod: not found"); return; }
    int p = 0;
    if (mode.length() == 3) {
        p |= (mode.charAt(0) == 'r') ? 0b100 : 0;
        p |= (mode.charAt(1) == 'w') ? 0b010 : 0;
        p |= (mode.charAt(2) == 'x') ? 0b001 : 0;
        rp.node.perms = p;
        save();
        System.out.println("perms -> " + permsToString(p));
    } else {
        System.out.println("chmod: use symbolic like rwx, r-x, rw-, ---");
    }
}

private void cmd_find(String name) {
    List<String> results = new ArrayList<>();
    dfsFind(fs.root, "/", name.toLowerCase(Locale.ROOT), results);
    if (results.isEmpty()) {
        System.out.println("find: no matches");
    } else {
        results.forEach(System.out::println);
    }
}

private void dfsFind(VNode node, String path, String needle, List<String> out) {
    String thisName = node == fs.root ? "/" : node.name;
    if (thisName.toLowerCase(Locale.ROOT).contains(needle)) out.add(path);
    if (node.directory) {
        for (VNode c : node.children.values()) {
            String childPath = path.equals("/") ? "/" + c.name : path + "/" + c.name;
            dfsFind(c, childPath, needle, out);
        }
    }
}

private void cmd_help() {
    System.out.println("Commands:\n" +
            "  pwd                               - print current directory\n" +
            "  cd <path>                         - change directory\n" +
            "  ls [path]                         - list directory or file\n" +
            "  tree [path]                       - show directory tree\n" +
            "  mkdir <path>                      - create directory (parents auto-created)\n" +
            "  touch <path>                      - create empty file or update mtime\n" +
            "  write <path> \"text...\"         - write/overwrite file content\n" +
            "  append <path> \"text...\"        - append content to file\n" +
            "  cat <path>                        - print file content\n" +
            "  rm <path>                         - remove file (or empty dir)\n" +
            "  rm -r <path>                      - remove directory recursively\n" +
            "  mv <src> <dst>                    - move/rename (dst may be dir or new name)\n" +
            "  cp <src> <dst>                    - copy file/dir (recursive)\n" +
            "  stat <path>                       - metadata\n" +
            "  chmod <path> <rwx>                - set perms (e.g., rw-, r-x, ---)\n" +
            "  find <name>                       - substring search by name\n" +
            "  save                              - flush to disk image\n" +
            "  help                              - show this help\n" +
            "  exit                              - save & quit\n");
}

private void repl() {
    System.out.println("Type 'help' for commands. Paths can be absolute (/a/b) or relative.");
    try (BufferedReader br = new BufferedReader(new InputStreamReader(System.in))) {
        while (true) {
            System.out.print("vfs:" + fs.cwd + "$ ");
            String line = br.readLine();
            if (line == null) break;
            line = line.trim();
            if (line.isEmpty()) continue;

            // Tokenize preserving quoted strings for write/append
            List<String> tokens = tokenize(line);
            if (tokens.isEmpty()) continue;
            String cmd = tokens.get(0);

            try {
                switch (cmd) {
                    case "pwd" -> cmd_pwd();
                    case "cd" -> { if (tokens.size() >= 2) cmd_cd(tokens.get(1)); else System.out.println("cd: missing path"); }
                    case "ls" -> cmd_ls(tokens.size() >= 2 ? tokens.get(1) : null);
                    case "tree" -> cmd_tree(tokens.size() >= 2 ? tokens.get(1) : null);
                    case "mkdir" -> { if (tokens.size() >= 2) cmd_mkdir(tokens.get(1)); else System.out.println("mkdir: missing path"); }
                    case "touch" -> { if (tokens.size() >= 2) cmd_touch(tokens.get(1)); else System.out.println("touch: missing path"); }
                    case "write" -> {
                        if (tokens.size() >= 3) cmd_write(tokens.get(1), tokens.get(2), false);
                        else System.out.println("write: usage write <path> \"text\"");
                    }
                    case "append" -> {
                        if (tokens.size() >= 3) cmd_write(tokens.get(1), tokens.get(2), true);
                        else System.out.println("append: usage append <path> \"text\"");
                    }
                    case "cat" -> { if (tokens.size() >= 2) cmd_cat(tokens.get(1)); else System.out.println("cat: missing path"); }
                    case "rm" -> {
                        if (tokens.size() == 3 && tokens.get(1).equals("-r")) cmd_rm(tokens.get(2), true);
                        else if (tokens.size() == 2) cmd_rm(tokens.get(1), false);
                        else System.out.println("rm: usage rm <path> | rm -r <path>");
                    }
                    case "mv" -> {
                        if (tokens.size() >= 3) cmd_mv(tokens.get(1), tokens.get(2));
                        else System.out.println("mv: usage mv <src> <dst>");
                    }
                    case "cp" -> {
                        if (tokens.size() >= 3) cmd_cp(tokens.get(1), tokens.get(2));
                        else System.out.println("cp: usage cp <src> <dst>");
                    }
                    case "stat" -> { if (tokens.size() >= 2) cmd_stat(tokens.get(1)); else System.out.println("stat: missing path"); }
                    case "chmod" -> {
                        if (tokens.size() >= 3) cmd_chmod(tokens.get(1), tokens.get(2));
                        else System.out.println("chmod: usage chmod <path> <rwx>");
                    }
                    case "find" -> { if (tokens.size() >= 2) cmd_find(tokens.get(1)); else System.out.println("find: missing name"); }
                    case "save" -> { save(); System.out.println("[saved]"); }
                    case "help" -> cmd_help();
                    case "exit" -> { save(); System.out.println("bye"); return; }
                    default -> System.out.println("unknown command: " + cmd + ". Try 'help'.");
             