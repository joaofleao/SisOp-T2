import java.io.File;
import java.util.Scanner;

public class Terminator{
    static int block_size = 1024;
	static int blocks = 2048;
	static int fat_size = blocks * 2;
	static int fat_blocks = fat_size / block_size;
	static int root_block = fat_blocks;
	static int dir_entry_size = 32;
	static int dir_entries = block_size / dir_entry_size;
	public static boolean isFolder = true;

	

	/* FAT data structure */
	final static short[] fat = new short[blocks];
	/* data block */
	final static byte[] data_block = new byte[block_size];



    public static void main(String args[]){
		Scanner in = new Scanner(System.in);
		String command = "";
		do{
			System.out.println("Digite um dos comandos existentes ou digite help");
			command = in.nextLine();
			setOperation(command);
		}while(!command.equals("exit"));


    }
	public static String getCommand(String text) {
		String command = "";
		for (int i = 0; text.charAt(i)!=' '; i++) {
			command = command + text.charAt(i);
		}
		return command;

	}
	public static String getExt(String text) {
		String ext = "";
		int i;
		for (i = 0; text.charAt(i)!=' '; i++) {}
		for (i = i; i<text.length(); i++) {
			ext = ext + text.charAt(i);
		}
		return ext;
	}

	public static void setOperation(String command){
		boolean another = false;
		String c;
		for (int i = 0; i<command.length(); i++) {
			if( command.charAt(i) == ' '){
				another = true;
			}
		}
		if(another){
			c = getCommand(command);
		}else c = command;
		switch(c){
			case "init": 
				System.out.println();
				initFat();
				System.out.println("FAT inicializada");
				break;
			case "load": 
				load();
				break;
			case "ls":
				String path = getExt(command);
				ls(path); 
				break;
			case "mkdir":
				String pathMk = getExt(command);
				mkdir(pathMk);
				break;
			case "create": 
				String pathCr = getExt(command);
				create(pathCr);
				break;
			case "unlink": 
				String pathUn = getExt(command);
				unlink(pathUn);
				break;
			case "right": 
				break;
			case "append": 
				break;
			case "read": 
				break;
			case "help": 
				System.out.println("Comandos Disponíveis: ");
				System.out.println("init - inicializar o sistema de arquivos com as estruturas de dados, semelhante a formatar o sistema de arquivos virtual");
				System.out.println("load - carregar o sistema de arquivos do disco");
				System.out.println("ls [/caminho/diretorio] - listar diretorio");
				System.out.println("mkdir [/caminho/diretorio] - criar diretorio");
				System.out.println("create [/caminho/arquivo] - criar arquivo");
				System.out.println("unlink [/caminho/arquivo] - excluir arquivo ou diretorio (o diretorio precisa estar vazio)");
				System.out.println("write ${string} [/caminho/arquivo] - escrever dados em um arquivo (sobrescrever dados)");
				System.out.println("append ${string} [/caminho/arquivo] - anexar dados em um arquivo");
				System.out.println("read [/caminho/arquivo] - ler o conteudo de um arquivo");
				System.out.println("exit - SAIR");
				System.out.println("help - mostrar comandos disponíveis");
				break;
			case "exit":
				break;
			default: 
				System.out.println("Comando nao reconhecido");
		}
	}

	/**
	 * inicializa a FAT e a root
	 */
	public static void initFat(){
		for (int i = 0; i < fat_blocks; i++)
			fat[i] = 0x7ffe;
		fat[root_block] = 0x7fff;
		for (int i = root_block + 1; i < blocks; i++)
			fat[i] = 0;
		FileSystem.writeFat("filesystem.dat", fat);
		for (int i = 0; i < block_size; i++)
			data_block[i] = 0;

		FileSystem.writeBlock("filesystem.dat", root_block, data_block);

		for (int i = root_block + 1; i < blocks; i++)
			FileSystem.writeBlock("filesystem.dat", i, data_block);
	}

	/**
	 * carrega a FAT
	 */
	public static void load(){
		final String dir = System.getProperty("user.dir");
		File f = new File(dir + "/filesystem.dat");
		short[] dirNew = FileSystem.readFat(f.getAbsolutePath());
		for (int i = 0; i < blocks; i++) {
			fat[i] = dirNew[i];
		}
		System.out.println("Load executado");
	}

	/**
	 * cria um arquivo passando seu caminho por parâmetro
	 *
	 * @param s path do arquivo
	 */
	public static void create(String path){
        int blockPrev = getBlock(path, true);
		if(isFolder == true){
			int blockEmpty = getFirstEmptyBlock();
			int entry = getEntry(blockPrev);
			
			String[] file = path.split("/");

			System.out.println("Path: " + path + " entry: " + entry + " blockPrev: " + blockPrev + " blockEmpty: " +  blockEmpty);
			DirEntry dir_entry = new DirEntry();
			String name = file[file.length-1];
			byte[] namebytes = name.getBytes();
			for (int i = 0; i < namebytes.length; i++)
				dir_entry.filename[i] = namebytes[i];
			dir_entry.attributes = 0x01;
			dir_entry.first_block = (short)blockEmpty;
			dir_entry.size = 222; //


			FileSystem.writeDirEntry(blockPrev, entry, dir_entry);

			fat[blockEmpty] = 0x22; //
			FileSystem.writeFat("filesystem.dat", fat);
		}else System.err.println("Anterior é arquivo, não é possivel criar um arquivo dentro de outro.");

	}
	
	/**
	 * @param s path do diretório
	 * lista o contéudo do diretório passado por parâmetro
	 */
    private static void ls(String s){
        int block = getBlock(s, false);

        if(block == -1){
            System.err.println("Caminho incorreto! ");
            return;
        }

        System.err.println("Conteúdo: ");
        for (int i = 0; i < 32; i++) {
			if(FileSystem.readDirEntry(block, i).attributes == 1){
            	System.out.println(i+1 + "\t" + new String(FileSystem.readDirEntry(block, i).filename) + "\t" + "arquivo" );
			}else if(FileSystem.readDirEntry(block, i).attributes == 2){
				System.out.println(i+1 + "\t" + new String(FileSystem.readDirEntry(block, i).filename) + "\t" + "pasta" );
			}else{
				System.out.println(i+1 + "\t" + new String(FileSystem.readDirEntry(block, i).filename) + "\t" + "vazio" );
			}
        }
        
	}

	/**
	 * cria um diretório passando seu caminho por parâmetro. Ele só cria um diretório dentro de outro, se o outro foi cri-
	 * ado anteriormente
	 *
	 * @param s path do diretório
	 */
	private static void mkdir(String path){
        int blockPrev = getBlock(path, true);
        int blockEmpty = getFirstEmptyBlock();
        int entry = getEntry(blockPrev);

        String[] file = path.split("/");

        System.out.println("Path: " + path + " entry: " + entry + " blockPrev: " + blockPrev + " blockEmpty: " +  blockEmpty);
        DirEntry dir_entry = new DirEntry();
		String name = file[file.length-1];
		byte[] namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x02;
		dir_entry.first_block = (short)blockEmpty;
		dir_entry.size = 222; //
        FileSystem.writeDirEntry(blockPrev, entry, dir_entry);

        fat[blockEmpty] = 0x22; //
        FileSystem.writeFat("filesystem.dat", fat);

    }

	private static void unlink(String path){
		int blockPrev = getBlock(path, true);
        int blockEmpty = getFirstEmptyBlock();
        int entry = getEntry(blockPrev);

        String[] file = path.split("/");

        System.out.println("Path: " + path + " entry: " + entry + " blockPrev: " + blockPrev + " blockEmpty: " +  blockEmpty);
        DirEntry dir_entry = new DirEntry();
		String name = file[file.length-1];
		byte[] namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = 0;
		dir_entry.attributes = 0;
		dir_entry.first_block = 0;
		dir_entry.size = 0; //
        FileSystem.writeDirEntry(blockPrev, entry, dir_entry);

        fat[blockEmpty] = 0; //
        FileSystem.writeFat("filesystem.dat", fat);
			
	}

	private static int getBlock(String s, boolean cond) {
        String[] path = s.split("/");
        

        int size = path.length;
        if(cond == true) size = path.length -1;
        int block = root_block;
        DirEntry entry;
        entry = FileSystem.readDirEntry(block, 0);

        for (int i = 1; i < size; i++) {
            for (int j = 0; j < 32; j++) {
                entry = FileSystem.readDirEntry(block, j);

                if( new String(entry.filename).trim().equals(path[i])){
					block = entry.first_block;
					if(entry.attributes == 2){
						isFolder = true;
					}else isFolder = false;
                    break;
                }
                if(j == 31) return -1;
            }
        }

        return block;
    }

    private static int getFirstEmptyBlock(){
        int block = -1;
        for (int i = 0; i < fat.length; i++) {
            if(fat[i] == 0){
                block = i;
                break;
            }
        }
        return block;
    }

    private static int getEntry(int block){
        int entry = -1;
        for (int i = 0; i < 32; i++) {
            String file = new String(FileSystem.readDirEntry(block, i).filename).trim();
           if(file.equals("")){
               entry = i;
               return entry;
           }
        }
        return entry;
    }
}