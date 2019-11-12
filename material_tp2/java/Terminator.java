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
        createFat();
        System.out.println("Fat criada");
		String command = "";
		do{
			System.out.println("Digite um dos comandos existentes ou digite help");
			command = in.nextLine();
			setOperation(command);
		}while(!command.equals("exit"));
    }

	public static void setOperation(String command){
		switch(command){
			case "init": 
				System.out.println("LUL");
				break;
			case "load": 
				break;
			case "ls": 
				break;
			case "mkdir": 
				break;
			case "create": 
				break;
			case "unlink": 
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
				System.out.println("help - mostrar comandos disponíveis");
				break;
					
		}
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
}