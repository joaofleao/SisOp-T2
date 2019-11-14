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
		
		switch(command){
			case "init": 
				createFat();
				System.out.println("LUL");
				break;
			case "load": 
				load();
				break;
			case "ls": 
				break;
			case "mkdir": 
				break;
			case "create": 
				create();
				System.out.println("foi");
				break;
			case "unlink": 
				break;
			case "right": 
				break;
			case "append": 
				break;
			case "find": 
				find("file1");
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
				System.out.println("help - mostrar comandos disponíveis");
				break;
			default: 
				System.out.println("Comando nao reconhecido");
		}
	}


	public static void load(){
		final String dir = System.getProperty("user.dir");
		File f = new File(dir + "/filesystem.dat");
		short[] dirNew = FileSystem.readFat(f.getAbsolutePath());
		for (int i = 0; i < blocks; i++) {
			fat[i] = dirNew[i];
		}
		System.out.println("Load executado");
	}
    public static void createFat(){
        /* initialize the FAT */
		for (int i = 0; i < fat_blocks; i++)
			fat[i] = 0x7ffe;
		fat[root_block] = 0x7fff;
		for (int i = root_block + 1; i < blocks; i++)
			fat[i] = 0;
		/* write it to disk */
		FileSystem.writeFat("filesystem.dat", fat);
        	/* initialize an empty data block */
		for (int i = 0; i < block_size; i++)
			data_block[i] = 0;

		/* write an empty ROOT directory block */
		FileSystem.writeBlock("filesystem.dat", root_block, data_block);

		/* write the remaining data blocks to disk */
		for (int i = root_block + 1; i < blocks; i++)
			FileSystem.writeBlock("filesystem.dat", i, data_block);
    }

	public static void create(){
		DirEntry dir_entry = new DirEntry();
		String name = "file1";
		byte[] namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 1111;
		dir_entry.size = 222;
		FileSystem.writeDirEntry(root_block, 0, dir_entry);

		name = "file2";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 2222;
		dir_entry.size = 333;
		FileSystem.writeDirEntry(root_block, 1, dir_entry);

		name = "file3";
		namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = 0x01;
		dir_entry.first_block = 3333;
		dir_entry.size = 444;
		FileSystem.writeDirEntry(root_block, 2, dir_entry);
	}

	public static void find(String s){
		DirEntry dir_entry = new DirEntry();
		/* list entries from the root directory */
		for (int i = 0; i < dir_entries; i++) {
			dir_entry = FileSystem.readDirEntry(root_block, i);
			if(new String(dir_entry.filename).trim().equals(s)){
				System.out.println("Achou " + new String(dir_entry.filename));
			}
		}
	}
}