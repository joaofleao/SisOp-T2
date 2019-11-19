import java.io.File;
import java.util.Arrays;
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

public class Terminator {
	static int block_size = 1024;
	static int blocks = 2048;
	static int fat_size = blocks * 2;
	static int fat_blocks = fat_size / block_size;
	static int root_block = fat_blocks;
	static int dir_entry_size = 32;
	static int dir_entries = block_size / dir_entry_size;
	public static boolean isFolder = true;
	public static boolean isFile = false;
	public static byte[] blockAux;

	public static void main(String args[]) {
		Scanner in = new Scanner(System.in);
		String command = "";
		do {
			System.out.println("Digite um dos comandos existentes ou digite help");
			System.out.println("Para passar caminhos para o terminal, comece com root/");
			command = in.nextLine();
			setOperation(command);

		} while (!command.equals("exit"));

	}

	public static String print(String[] vetor) {
		String txt = "";
		for (int i = 0; i < vetor.length; i++) {
			txt = txt + "." + vetor[i];
		}
		return txt;
	}

	public static String[] getCommand(String text) {
		String[] command = text.split(" ");
		return command;
	}

	public static void setOperation(String command) {
		switch (getCommand(command)[0]) {
			case "init":
				if (getCommand(command).length != 1) {
					System.out.println("Commando incorreto");
					break;
				}
				init();
				System.out.println("FAT inicializada");
				break;
			case "load":
				if (getCommand(command).length != 1) {
					System.out.println("Commando incorreto");
					break;
				}
				load();
				break;
			case "ls":
				if (getCommand(command).length != 2) {
					System.out.println("Commando incorreto");
					break;
				}
				String path = getCommand(command)[1];
				ls(path);
				break;
			case "mkdir":
				if (getCommand(command).length != 2) {
					System.out.println("Commando incorreto");
					break;
				}
				String pathMk = getCommand(command)[1];
				mkdir(pathMk);
				break;
			case "create":
				if (getCommand(command).length != 2) {
					System.out.println("Commando incorreto");
					break;
				}
				String pathCr = getCommand(command)[1];
				create(pathCr);
				break;
			case "unlink":
				if (getCommand(command).length != 2) {
					System.out.println("Commando incorreto");
					break;
				}
				String pathUn = getCommand(command)[1];
				unlink(pathUn);
				break;
			case "write":
				if (getCommand(command).length != 3) {
					System.out.println("Commando incorreto");
					break;
				}
				write(getCommand(command)[2], getCommand(command)[1]);
				break;
			case "append":
				if (getCommand(command).length != 3) {
					System.out.println("Commando incorreto");
					break;
				}
				append(getCommand(command)[2], getCommand(command)[1]);
				break;
			case "read":
				if (getCommand(command).length != 2) {
					System.out.println("Commando incorreto");
					break;
				}
				String pathRe = getCommand(command)[1];
				read(pathRe);
				break;
			case "clear":
				System.out.print("\033[H\033[2J");  
				System.out.flush();  
				break;
			case "help": 
				if (getCommand(command).length!=1) {
					System.out.println("Commando incorreto");
					break;
				}
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
				if (getCommand(command).length!=1) {
					System.out.println("Commando incorreto");
					break;
				}
				break;
			default: 
				System.out.println("Comando nao reconhecido");
		}
	}

	/**
	 * inicializa a FAT e a root
	 */
	public static void init(){
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
	 * @param path path do diretório
	 * lista o contéudo do diretório passado por parâmetro
	 */
    private static void ls(String path){
		int block = getBlock(path, false);

        if(block == -1){
            System.err.println("Caminho incorreto! \n");
            return;
        }

		if(isFile){
			System.err.println("ls não é permitido em um arquivo \n");
		}else{
			System.out.println("Conteúdo do diretório: ");
			for (int i = 0; i < 32; i++) {
				if(readDirEntry(block, i).attributes == 1){
					System.out.println("bloco: " + (i+1) + "\t" + " nome: " + new String(readDirEntry(block, i).filename) + "\t\t tipo: " + "arquivo" );
				}else if(readDirEntry(block, i).attributes == 2){
					System.out.println("bloco: " + (i+1) + "\t" + " nome: " + new String(readDirEntry(block, i).filename) + "\t\t tipo: " + "pasta" );
				}else if(readDirEntry(block, i).attributes == 0){
					System.out.println("bloco: " + (i+1) + "\t" + " nome: " +  new String(readDirEntry(block, i).filename) + "\t\t tipo: " + "vazio" );
				}else{
					System.out.println("bloco: " + (i+1) + "\t" + " nome: " +  new String(readDirEntry(block, i).filename) + "\t\t tipo: " + "conteudo do arquivo" );
				}
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
		int previousBlock = getBlock(path, true);

        if(previousBlock == -1){
            System.err.println("Caminho incorreto! ");
            return;
        }
		
		if(isFile == false){
			int blockEmpty = getFirstEmptyBlock();
			int entry = getEntry(previousBlock);
	
			String[] file = path.split("/");
			
	
			System.out.println("Path: " + path + " entry: " + entry + " previousBlock: " + previousBlock + " blockEmpty: " +  blockEmpty);
			DirEntry dir_entry = new DirEntry();
			String name = file[file.length-1];
			byte[] namebytes = name.getBytes();
			for (int i = 0; i < namebytes.length; i++)
				dir_entry.filename[i] = namebytes[i];
			dir_entry.attributes = 0x02;
			dir_entry.first_block = (short)blockEmpty;
			dir_entry.size = 0; //
	
	

			
			blockAux = readBlock("filesystem.dat", previousBlock);

			writeDirEntry(previousBlock, entry, dir_entry, blockAux);
	
			fat[blockEmpty] = 0x7fff; //
			writeFat("filesystem.dat", fat);
			
		}else System.err.println("Anterior é um arquivo. Não é possível criar uma pasta dentro de um arquivo");
    }
	
	/**
	 * cria um arquivo passando seu caminho por parâmetro
	 *
	 * @param path path do arquivo
	 */
	public static void create(String path){
		//validar se a string é vazia
		if(path.isEmpty()) System.err.println("Por favor, informe um caminho");



		isFolder=true;
        int previousBlock = getBlock(path, true);
		if(isFolder == true){
			int blockEmpty = getFirstEmptyBlock();
			int entry = getEntry(previousBlock);
			
			String[] file = path.split("/");

			System.out.println("Path: " + path + " entry: " + entry + " previousBlock: " + previousBlock + " blockEmpty: " +  blockEmpty);
			DirEntry dir_entry = new DirEntry();
			String name = file[file.length-1];
			byte[] namebytes = name.getBytes();
			for (int i = 0; i < namebytes.length; i++)
				dir_entry.filename[i] = namebytes[i];
			dir_entry.attributes = 0x01;
			dir_entry.first_block = (short)blockEmpty;
			dir_entry.size = 0; //

			
	
			blockAux = readBlock("filesystem.dat", previousBlock);

			writeDirEntry(previousBlock, entry, dir_entry, blockAux);
			


			fat[blockEmpty] = 0x7fff; //
			writeFat("filesystem.dat", fat);
		}else System.err.println("Anterior é arquivo, não é possivel criar um arquivo dentro de outro.");

	}

	/**
	 * @param path caminho do arquivo
	 * 
	 * Deleta um arquivo
	 * Somente deleta pastas se estiverem vazias 
	 */ 
	private static void unlink(String path){
		int previousBlock = getBlock(path, true);
		int currentBlock = getBlock(path, false);
        int blockEmpty = getFirstEmptyBlock();
		int entry = getEntry(previousBlock);
		
		if(isFolder){
			for (int i = 0; i < 32; i++) {
				String file = new String(readDirEntry(currentBlock, i).filename).trim();
				if(!file.equals("")){
					System.err.println("Esta pasta contem arquivos. Remova-os");
					return;
				}
			}
		}

		String[] file = path.split("/");

        System.out.println("Path: " + path + " entry: " + entry + " blockPrev: " + previousBlock + " blockEmpty: " +  blockEmpty);
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

		blockAux = readBlock("filesystem.dat", previousBlock);

        writeDirEntry(previousBlock, entry-1, dir_entry, blockAux);

        fat[blockEmpty] = 0; //
		writeFat("filesystem.dat", fat);
	}
	
	//PROBLEMAS: quando write sobrescreve com um valor > 25 ele remove o segundo bloco
	/**
	 * @param path caminho do arquivo
	 * @param content conteudo para ser inserido no arquivo
	 * Escreve dados dentro de um arquivo e sobrescreve se ja existir algo.
	 */
	private static void write(String path, String content){
		int currentBlock = getBlock(path, false);
		int blockEmpty = getFirstEmptyBlock();
		// int entry = getEntry(currentBlock);

		if(currentBlock == -1){
            System.err.println("Caminho incorreto! ");
            return;
		}

		

        DirEntry dir_entry = new DirEntry();
		String name = content;
		byte[] namebytes = name.getBytes();
		byte[] aux = new byte[25];
		int cont = 1;
		int j = 0;
		int y = 0;
		int test=0;
		if(namebytes.length>=25){
			for (int i = 1; i <= namebytes.length; i++) {
				aux[j] = namebytes[y];
				j++;
				y++;
				if(i == cont * 25 || i == namebytes.length){
					System.out.println(Arrays.toString(aux));
					dir_entry.filename = aux;
					dir_entry.attributes = 3;
					dir_entry.first_block = (short)blockEmpty;
					dir_entry.size = 222; //
			
					blockAux = readBlock("filesystem.dat", currentBlock);

					writeDirEntry(currentBlock, test, dir_entry, blockAux);
					test++;
					fat[blockEmpty] = 0x7fff; //
					writeFat("filesystem.dat", fat);					

					cont++; 
					j = 0;
					for (int k = 0; k < aux.length; k++) {
						aux[k] = 0;
					}
					System.out.println(Arrays.toString(aux));
				}
			}
		}else{
			for (int i = 0; i < namebytes.length; i++){
				dir_entry.filename[i] = namebytes[i];
			}

			dir_entry.attributes = 3;
			dir_entry.first_block = (short)blockEmpty;
			dir_entry.size = 222; //
	
			
			blockAux = readBlock("filesystem.dat", currentBlock);

			writeDirEntry(currentBlock, 0, dir_entry, blockAux);
	
			fat[blockEmpty] = 0x7fff; //
			writeFat("filesystem.dat", fat);					

		}


	}
	
	//PROBLEMAS: quando existia um texto maior que 25 o append começa a partir do 25, portanto sobrescreve os outros
	//blocos e deixa o do 25 p tras
	/**
	 * 
	 * @param path caminho do arquivo
	 * @param content conteudo do arquivo
	 * Insere dados no arquivo não sobrescrevendo o que já existe
	 */
	public static void append(String path, String content){
		int currentBlock = getBlock(path, false);
		int blockEmpty = getFirstEmptyBlock();
		int entry = getEntry(currentBlock);

		if(currentBlock == -1){
            System.err.println("Caminho incorreto! ");
            return;
		}

        DirEntry dir_entry = new DirEntry();
		String name = content;
		byte[] namebytes = name.getBytes();
		byte[] aux = new byte[25];
		int cont = 1;
		int j = 0;
		int y = 0;
		if(namebytes.length>=25){
			for (int i = 1; i <= namebytes.length; i++) {
				aux[j] = namebytes[y];
				j++;
				y++;
				if(i == cont * 25 || i == namebytes.length){
					System.out.println(Arrays.toString(aux));
					dir_entry.filename = aux;
					dir_entry.attributes = 3;
					dir_entry.first_block = (short)blockEmpty;
					dir_entry.size = 222; //
			
					
					blockAux = readBlock("filesystem.dat", currentBlock);

					writeDirEntry(currentBlock, entry, dir_entry, blockAux);
					fat[blockEmpty] = 0x7fff; //
					writeFat("filesystem.dat", fat);					

					cont++; 
					j = 0;
					for (int k = 0; k < aux.length; k++) {
						aux[k] = 0;
					}
					System.out.println(Arrays.toString(aux));
				}
			}
		}else{
			for (int i = 0; i < namebytes.length; i++){
				dir_entry.filename[i] = namebytes[i];
			}

			dir_entry.attributes = 3;
			dir_entry.first_block = (short)blockEmpty;
			dir_entry.size = 222; //
			
		

			blockAux = readBlock("filesystem.dat", currentBlock);

			writeDirEntry(currentBlock, entry, dir_entry, blockAux);
	
			fat[blockEmpty] = 0x7fff; //
			writeFat("filesystem.dat", fat);					

		}
	}
	
	/**
	 * @param path caminho do arquivo
	 * le dados do arquivo
	 */
	public static void read(String path){
		int block = getBlock(path, false);
		

        if(block == -1){
            System.err.println("Caminho incorreto! ");
            return;
        }

        System.out.println("Conteúdo: ");
        for (int i = 0; i < 32; i++) {
			if(readDirEntry(block, i).attributes == 3){
				System.out.println("bloco: " + (i+1) + "\t" + new String(readDirEntry(block, i).filename) + "\t" + "conteudo do arquivo" );
			}
        }
	}
	
	






	//Utils
	
	/**
	 * 
	 * @param pathFromCommand caminho do arquivo ou pasta
	 * @param cond se precisa retornar o bloco atual ou o pai dele
	 * @return bloco
	 * Percorre os blocos, até achar o buscado
	 */
	private static int getBlock(String pathFromCommand, boolean cond) {
        String[] path = pathFromCommand.split("/");
        

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
						isFile = false;
					}else{
						isFolder = false;
						isFile = true;
					} 
                    break;
                }
                if(j == 31) return -1;
            }
        }

        return block;
	}
	
	/**
	 * @return primeiro bloco vazio da FAT
	 * Procura pelo primeiro bloco vazio da FAT
	 */
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

	/**
	 * 
	 * @param block bloco que deve ser olhada suas 32 entradas
	 * @return primeira entrada vazia
	 * Procura pela primeira entrada vazia dentro de um bloco específico
	 */
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
	public static void writeDirEntry(int block, int entry, DirEntry dir_entry, byte[] data_block) {
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