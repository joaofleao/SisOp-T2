import java.io.File;
import java.util.Scanner;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.DataOutputStream;
import java.io.DataInputStream;
import java.io.FilterInputStream;

public class Terminator{
    static int block_size = 1024;
	static int blocks = 2048;
	static int fat_size = blocks * 2;
	static int fat_blocks = fat_size / block_size;
	static int root_block = fat_blocks;
	static int dir_entry_size = 32;
	static int dir_entries = block_size / dir_entry_size;
	public static boolean isFolder = true;

	

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
		writeFat("filesystem.dat", fat);
		for (int i = 0; i < block_size; i++)
			data_block[i] = 0;

		writeBlock("filesystem.dat", root_block, data_block);

		for (int i = root_block + 1; i < blocks; i++)
			writeBlock("filesystem.dat", i, data_block);
	}

	/**
	 * carrega a FAT
	 */
	public static void load(){
		final String dir = System.getProperty("user.dir");
		File f = new File(dir + "/filesystem.dat");
		short[] dirNew = readFat(f.getAbsolutePath());
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
			dir_entry.size = 0; //

			
			for (int i = 0; i < block_size; i++) {
				data_block[i] = 0;
			}

			writeDirEntry(blockPrev, entry, dir_entry);
			


			fat[blockEmpty] = dir_entry.first_block; //
			writeFat("filesystem.dat", fat);
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
			if(readDirEntry(block, i).attributes == 1){
            	System.out.println(i+1 + "\t" + new String(readDirEntry(block, i).filename) + "\t" + "arquivo" );
			}else if(readDirEntry(block, i).attributes == 2){
				System.out.println(i+1 + "\t" + new String(readDirEntry(block, i).filename) + "\t" + "pasta" );
			}else{
				System.out.println(i+1 + "\t" + new String(readDirEntry(block, i).filename) + "\t" + "vazio" );
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
		dir_entry.size = 0; //


		for (int i = 0; i < block_size; i++) {
			data_block[i] = 0;
		}

        writeDirEntry(blockPrev, entry, dir_entry);

        fat[blockEmpty] = 0x7fff; //
		writeFat("filesystem.dat", fat);
		
    }
	//corrigir unlink
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


		for (int i = 0; i < block_size; i++) {
			data_block[i] = 0;
		}

        writeDirEntry(blockPrev, entry-1, dir_entry);

        fat[blockEmpty] = 0; //
		writeFat("filesystem.dat", fat);
	}
	

	private static int getBlock(String s, boolean cond) {
        String[] path = s.split("/");
        

        int size = path.length;
        if(cond == true) size = path.length -1;
        int block = root_block;
        DirEntry entry;
        entry = readDirEntry(block, 0);

        for (int i = 1; i < size; i++) {
            for (int j = 0; j < 32; j++) {
                entry = readDirEntry(block, j);

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
            String file = new String(readDirEntry(block, i).filename).trim();
           if(file.equals("")){
               entry = i;
               return entry;
           }
        }
        return entry;
	}
	



	//metodos professor
	
	/* FAT data structure */
	final static short[] fat = new short[blocks];
	/* data block */
	final static byte[] data_block = new byte[block_size];

	public static byte[] readBlock(String file, int block) {
		byte[] record = new byte[block_size];
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(block * block_size);
			fileStore.read(record, 0, block_size);
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return record;
	}

	/* writes a data block to disk */
	public static void writeBlock(String file, int block, byte[] record) {
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(block * block_size);
			fileStore.write(record, 0, block_size);
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* reads the FAT from disk */
	public static short[] readFat(String file) {
		short[] record = new short[blocks];
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(0);
			for (int i = 0; i < blocks; i++)
				record[i] = fileStore.readShort();
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return record;
	}

	/* writes the FAT to disk */
	public static void writeFat(String file, short[] fat) {
		try {
			RandomAccessFile fileStore = new RandomAccessFile(file, "rw");
			fileStore.seek(0);
			for (int i = 0; i < blocks; i++)
				fileStore.writeShort(fat[i]);
			fileStore.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/* reads a directory entry from a directory */
	public static DirEntry readDirEntry(int block, int entry) {
		byte[] bytes = readBlock("filesystem.dat", block);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		DataInputStream in = new DataInputStream(bis);
		DirEntry dir_entry = new DirEntry();

		try {
			in.skipBytes(entry * dir_entry_size);

			for (int i = 0; i < 25; i++)
				dir_entry.filename[i] = in.readByte();
			dir_entry.attributes = in.readByte();
			dir_entry.first_block = in.readShort();
			dir_entry.size = in.readInt();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return dir_entry;
	}

	/* writes a directory entry in a directory */
	public static void writeDirEntry(int block, int entry, DirEntry dir_entry) {
		byte[] bytes = readBlock("filesystem.dat", block);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		DataInputStream in = new DataInputStream(bis);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		DataOutputStream out = new DataOutputStream(bos);

		try {
			for (int i = 0; i < entry * dir_entry_size; i++)
				out.writeByte(in.readByte());

			for (int i = 0; i < dir_entry_size; i++)
				in.readByte();

			for (int i = 0; i < 25; i++)
				out.writeByte(dir_entry.filename[i]);
			out.writeByte(dir_entry.attributes);
			out.writeShort(dir_entry.first_block);
			out.writeInt(dir_entry.size);

			for (int i = entry + 1; i < entry * dir_entry_size; i++)
				out.writeByte(in.readByte());
		} catch (IOException e) {
			e.printStackTrace();
		}

		byte[] bytes2 = bos.toByteArray();
		for (int i = 0; i < bytes2.length; i++)
			data_block[i] = bytes2[i];
		writeBlock("filesystem.dat", block, data_block);
	}

}