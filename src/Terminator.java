import java.io.File;
import java.util.Arrays;
import java.util.Scanner;
import java.util.Stack;

import javax.swing.text.AttributeSet.ColorAttribute;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.io.ByteArrayOutputStream;
import java.awt.Color;
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
	static Stack<String> folders = new Stack<String>();	
	public static boolean isFolder = true;
	public static boolean isFile = false;
	public static byte[] blockAux;
	public static final String GREEN = "\u001B[32m";
	public static final String CYAN = "\u001B[36m";
	public static final String RESET = "\u001B[0m";
	public static final String RED = "\u001B[31m";
	public static final String BLUE = "\u001B[34m";

	public static void main(String args[]) {
		clear();
		Scanner in = new Scanner(System.in);
		String command = "";
		folders.push("root");
		do {
			System.out.println(RED + "THE TERMINATOR" + RESET);
			System.out.println(GREEN + "Diretorio atual: " + RESET + CYAN + getFullPath() + RESET);
			
			command = in.nextLine();
			clear();
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

	public static String getFullPath() {
		String path = "";
		for (int i = 0; i < folders.size(); i++) {
			path = path + folders.get(i) + "/";
		}
		return path;
	}
	public static void cd(String txt) {
		String[] path = txt.split("/");
		if (path[0].equals("..")) {
			if(folders.peek().equals("root")) return;
			folders.pop();
			return;
		} 
		String validate = getFullPath()+txt;
		int currentBlock = getBlock(validate, false);
		if(currentBlock == -1){
            System.err.println(RED + "Erro: " + RESET + "Caminho incorreto! ");
            return;
		}	
		
		if (txt.length()==0) {
			folders.clear();
			folders.push("root");
		}
		else {
			for (int i = 0; i < path.length; i++) {
				folders.push(path[i]);
			}
		}
	}

	public static void setOperation(String command) {
		switch (getCommand(command)[0]) {
			case "cd":
				
				if (getCommand(command).length==1) {
					cd("");
				}
				else if (getCommand(command).length==2) {
					String pathCD = getCommand(command)[1];
					cd(pathCD);
				}
				else {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
					break;
				}
				break;
			case "init":

				if (getCommand(command).length != 1) {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
					break;
				}
				System.out.println("Inicializando...");
				init();
				clear();
				System.out.println("Sistema de arquivos inicializado");
				break;
			case "load":

				if (getCommand(command).length != 1) {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
					break;
				}
				load();
				break;

			case "ls":
	
				if (getCommand(command).length > 2) {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
					break;
				}
				String path = "";
				if (getCommand(command).length==2) {
					path = getCommand(command)[1];
				}
				System.out.println(getFullPath()+ path);
				ls(getFullPath()+ path);
				break;

			case "mkdir":

				if (getCommand(command).length > 2) {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
					break;
				}
				path = "";
				if (getCommand(command).length==2) {
					path = getCommand(command)[1];
				}
				System.out.println(getFullPath()+ path);
				mkdir(getFullPath()+ path);
				break;

			case "create":
				if (getCommand(command).length != 2) {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
					break;
				}
				path = getCommand(command)[1];
				create(getFullPath()+ path);
				break;
			case "unlink":
				if (getCommand(command).length != 2) {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
					break;
				}
				path = getCommand(command)[1];
				unlink(getFullPath()+ path);
				System.out.println("Item deletado");
				break;
			case "write":
				String content = "";
				for (int i = 1; i < getCommand(command).length-1; i++) {
					content = content + getCommand(command)[i]+ " ";
				}
				write(getFullPath() + getCommand(command)[getCommand(command).length-1], content);
				System.out.println("Arquivo escrito");
				break;
			case "append":
				content = "";
				for (int i = 1; i < getCommand(command).length-1; i++) {
					content = content + getCommand(command)[i]+ " ";
				}	
				append(getFullPath() + getCommand(command)[getCommand(command).length-1], content);
				System.out.println("Texto adicionado com sucesso");
				break;
			case "read":
				if (getCommand(command).length != 2) {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
					break;
				}
				path = getCommand(command)[1];
				
				System.out.println(getFullPath()+ path);
				read(getFullPath()+ path);
				break;

			case "clear":
				clear();

				break;
			case "help": 
				if (getCommand(command).length!=1) {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
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
				System.out.println("clear - limpar a tela");
				System.out.println("help - mostrar comandos disponíveis");
				break;
			case "exit":
				if (getCommand(command).length!=1) {
					System.err.println(RED + "Erro: " + RESET + "Comando incorreto");
					break;
				}
				break;
			default: 
				System.out.println("Comando nao reconhecido");
		}
	}
	public static void clear() {
		System.out.print("\033[H\033[2J");  
		System.out.flush();  
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
		isFile = false;
		int block = getBlock(path, false);
		int cont = 0;
        if(block == -1){
            System.err.println(RED + "Erro: " + RESET + "Caminho incorreto! \n");
            return;
        }

		if(isFile){
			System.err.println(RED + "Erro: " + RESET + "ls não é permitido em um arquivo \n");
		}else{
			System.out.println("Conteúdo do diretório: \n");
			for (int i = 0; i < 32; i++) {
				if(readDirEntry(block, i).attributes == 1){
					System.out.println("bloco: " + (i+1) + "\t" + " nome: " + new String(readDirEntry(block, i).filename) + "\t\t tipo: " + "arquivo" );
					cont++;
				}else if(readDirEntry(block, i).attributes == 2){
					System.out.println("bloco: " + (i+1) + "\t" + " nome: " + new String(readDirEntry(block, i).filename) + "\t\t tipo: " + "pasta" );
					cont++;
				}
			}
		}
		if(cont == 0) System.out.println("\nPasta vazia\n");
        
	}

	/**
	 * cria um diretório passando seu caminho por parâmetro. Ele só cria um diretório dentro de outro, se o outro foi cri-
	 * ado anteriormente
	 *
	 * @param s path do diretório
	 */
	private static void mkdir(String path){
		isFile = false;
		int previousBlock = getBlock(path, true);
		int currentBlock = getBlock(path, false);

        if(previousBlock == -1){
            System.err.println(RED + "Erro: " + RESET + "Caminho incorreto! ");
            return;
		}
		else if(currentBlock != -1){
            System.err.println(RED + "Erro: " + RESET + "Já existe uma pasta com esse nome! ");
            return;
        }
		
		if(isFile == false){
			int blockEmpty = getFirstEmptyBlock();
			int entry = getEntry(previousBlock);
	
			String[] file = path.split("/");
			
			DirEntry dir_entry = defineEntry(file, 0x02, blockEmpty, 0);
			
			blockAux = readBlock("filesystem.dat", previousBlock);

			writeDirEntry(previousBlock, entry, dir_entry, blockAux);
	
			fat[blockEmpty] = 0x7fff; //
			writeFat("filesystem.dat", fat);
			System.out.println("Pasta '" + file[file.length-1]  + "' criada\n");
			
		}else System.err.println(RED + "Erro: " + RESET + "Anterior é um arquivo. Não é possível criar uma pasta dentro de um arquivo");
	}

	/**
	 * cria um arquivo passando seu caminho por parâmetro
	 *
	 * @param path path do arquivo
	 */
	public static void create(String path){
		isFolder=true;
		int previousBlock = getBlock(path, true);
		int currentBlock = getBlock(path, false);

        if(previousBlock == -1){
            System.err.println(RED + "Erro: " + RESET + "Caminho incorreto! ");
            return;
		}
		if(currentBlock != -1){
            System.err.println(RED + "Erro: " + RESET + "Já existe um arquivo com esse nome! ");
            return;
        }

		if(isFolder == true){
			int blockEmpty = getFirstEmptyBlock();
			int entry = getEntry(previousBlock);
			
			String[] file = path.split("/");

			DirEntry dir_entry = defineEntry(file, 0x01, blockEmpty, 0);
	
			blockAux = readBlock("filesystem.dat", previousBlock);

			writeDirEntry(previousBlock, entry, dir_entry, blockAux);
			


			fat[blockEmpty] = 0x7fff; //
			writeFat("filesystem.dat", fat);
			System.out.println("Arquivo '" + file[file.length-1] + "' criado\n");
		}else System.err.println(RED + "Erro: " + RESET + "Anterior é arquivo, não é possivel criar um arquivo dentro de outro.");

	}

	/**
	 * @param path caminho do arquivo
	 * 
	 * Deleta um arquivo
	 * Somente deleta pastas se estiverem vazias 
	 */ 
	private static void unlink(String path){
		int previousBlock = getBlock(path, true);

		if(previousBlock == -1){
            System.err.println(RED + "Erro: " + RESET + "Caminho incorreto! ");
            return;
		}

		int currentBlock = getBlock(path, false);
        int blockEmpty = getFirstEmptyBlock();
		int entry = 0;
		if(path.equals("root")){
			System.err.println(RED + "Erro: " + RESET + "Não é possível deletar a root");
			return;
		}
		if(isFolder){
			for (int i = 0; i < 32; i++) {
				String file = new String(readDirEntry(currentBlock, i).filename).trim();
				if(!file.equals("")){
					System.err.println(RED + "Erro: " + RESET + "Esta pasta contem conteúdo. Remova-os");
					return;
				}
			}
		}
		String[] file = path.split("/");
		DirEntry getEntry = new DirEntry();
		for (int i = 0; i < 32; i++) {
			getEntry = FileSystem.readDirEntry(previousBlock, i);
            String fileName = new String(getEntry.filename).trim();
			
            if (fileName.equals(file[file.length - 1])) {
				entry = i;
                break;
            }
        }
		
		String[] name = {""}; 
		DirEntry dir_entry = defineEntry(file, 0, 0, 0);

		//limpar fat e bloco
		byte[] blockByte = new byte[1024];
        short pos = fat[currentBlock];
        fat[currentBlock] = 0x0000;
        FileSystem.writeBlock("filesystem.dat", currentBlock, blockByte);

        while(pos != 0x7fff){
            short aux = fat[pos];
            fat[pos] = 0x0000;
            FileSystem.writeBlock("filesystem.dat", pos, blockByte);
            pos = aux;
        }

        writeFat("filesystem.dat", fat);

		blockAux = readBlock("filesystem.dat", previousBlock);

        writeDirEntry(previousBlock, entry, dir_entry, blockAux);

	}
	
	/**
	 * @param path caminho do arquivo
	 * @param content conteudo para ser inserido no arquivo
	 * Escreve dados dentro de um arquivo e sobrescreve se ja existir algo.
	 */
	private static void write(String path, String content){
		int currentBlock = getBlock(path, false);
		int blockEmpty = getFirstEmptyBlock();
		String name = content;
		byte[] namebytes = name.getBytes();
		byte[] aux = new byte[1024];
		int cont = 1;
		int j = 0;
		int y = 0;
		int test=0;
		int m=0; 

		if(currentBlock == -1){
            System.err.println(RED + "Erro: " + RESET + "Caminho incorreto! ");
            return;
		}

		
		//limpa fat e bloco
		byte[] blockByte = new byte[1024];
		short pos = fat[currentBlock];
		writeBlock("filesystem.dat", currentBlock, blockByte);

		while(pos != 0x7fff){
			short aux1 = fat[pos];
			fat[pos] = 0x0000;
			writeBlock("filesystem.dat", pos, blockByte);
			pos = aux1;
		}

		int plus = 1;
		byte[] hasMore = readBlock("filesystem.dat", currentBlock);
		if(hasMore[hasMore.length-1]==0) {
			System.out.println("ENTROU");
			for (int i = 0; i < hasMore.length+1; i++) {
				if(i == plus * 1024 || i == namebytes.length){
					writeBlock("filesystem.dat", currentBlock+plus, blockByte);
					hasMore =  readBlock("filesystem.dat", currentBlock+plus);
				}
			}
			writeFat("filesystem.dat", fat);
		}




		int u = 0;
		if(namebytes.length>=1024){
			for (int i = 1; i <= namebytes.length; i++) {
				aux[j] = namebytes[y];
				j++;
				y++;
				if(i == cont * 1024 || i == namebytes.length){
					writeBlock("filesystem.dat", currentBlock+u, aux);
					u++;
					test++;
					if(aux[aux.length-1] == 0){
						fat[blockEmpty] = 0x7fff;
						writeFat("filesystem.dat", fat);					
					}else{
						fat[blockEmpty] = (short)(blockEmpty + 1); //
						writeFat("filesystem.dat", fat);
						blockEmpty++;
					}
					m++;
					cont++; 
					j = 0;
					for (int k = 0; k < aux.length; k++) {
						aux[k] = 0;
					}

				}
			}
		}else{
			for (int i = 0; i < namebytes.length; i++) {
				aux[i] = namebytes[i];				
			}
			writeBlock("filesystem.dat", currentBlock, aux);

			fat[blockEmpty] = 0x7fff; //
			writeFat("filesystem.dat", fat);					

		}
	}
	/**
	 * 
	 * @param path caminho do arquivo
	 * @param content conteudo do arquivo
	 * Insere dados no arquivo não sobrescrevendo o que já existe
	 */
	public static void append(String path, String content){
		int currentBlock = getBlock(path, false);
		int blockEmpty = getFirstEmptyBlock();
		int entry = 0;

		if(currentBlock == -1){
            System.err.println(RED + "Erro: " + RESET + "Caminho incorreto! ");
            return;
		}
		int u = 0;
		String name = content;
		byte[] namebytes = name.getBytes();
		byte[] aux = new byte[1024];
		int cont = 1;
		int j = 0;
		int y = 0;
		byte[] readBlock = readBlock("filesystem.dat", currentBlock);
		for (int z = 0; z < readBlock.length; z++) {
			aux[z] = readBlock[z];
			if(readBlock[z] == 0){
				entry = z;
				break;
			}
		}
		if(namebytes.length>=1024){
			for (int i = 1; i <= namebytes.length; i++) {
				if(aux[aux.length-1] == 0){
					aux[entry] = namebytes[y];
				}
				entry++;
				j++;
				y++;
				if(i == cont * 1024 || i == namebytes.length){

					writeBlock("filesystem.dat", currentBlock + u, aux);
					u++;

					blockAux = readBlock("filesystem.dat", currentBlock);
					if(aux[aux.length-1] == 0){

						fat[blockEmpty] = 0x7fff;
						writeFat("filesystem.dat", fat);					
					}else{

						fat[blockEmpty] = (short)(blockEmpty + 1); //

						writeFat("filesystem.dat", fat);
						blockEmpty++;
					}

					writeFat("filesystem.dat", fat);					

					cont++; 
					j = 0;
					for (int k = 0; k < aux.length; k++) {
						aux[k] = 0;
					}
					entry=0;
				}
			}
		}else{
			int p=0;
			for (int i = entry; p < namebytes.length; i++) {
				aux[i] = namebytes[p];	
				p++;			
			}
			writeBlock("filesystem.dat", currentBlock, aux);
	
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
		
		byte[] text;
        text = FileSystem.readBlock("filesystem.dat", block);
		String readText = new String(text);
		
		byte[] text2;
        text2 = FileSystem.readBlock("filesystem.dat", block+1);
		String readText2 = new String(text2);

		byte[] text3;
        text3 = FileSystem.readBlock("filesystem.dat", block+2);
		String readText3 = new String(text3);

		byte[] text4;
        text4 = FileSystem.readBlock("filesystem.dat", block+3);
		String readText4 = new String(text3);

        if(block == -1){
            System.err.println(RED + "Erro: " + RESET + "Caminho incorreto! ");
            return;
		}
		
		short j = fat[block];
		short s = fat[block+1];

		text = FileSystem.readBlock("filesystem.dat", j);
		text2 = FileSystem.readBlock("filesystem.dat", s);
		text3 = FileSystem.readBlock("filesystem.dat", s+1);
		text4 = FileSystem.readBlock("filesystem.dat", s+3);

		readText = readText + new String(text);
		readText2 = readText2 + new String(text2);
		readText3 = readText3 + new String(text3);
		readText4 = readText3 + new String(text4);

        System.out.println(readText + readText2 + readText3 + readText4);

}
	
	






	//Utils

	public static DirEntry defineEntry(String[] fileName, int attr, int firstBlock, int size){
		DirEntry dir_entry = new DirEntry();
		String name = fileName[fileName.length-1];
		byte[] namebytes = name.getBytes();
		for (int i = 0; i < namebytes.length; i++)
			dir_entry.filename[i] = namebytes[i];
		dir_entry.attributes = (byte)attr;
		dir_entry.first_block = (short)firstBlock;
		dir_entry.size = size; //

		return dir_entry;
	}
	


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