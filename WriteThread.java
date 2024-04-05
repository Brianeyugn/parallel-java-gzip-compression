import java.util.zip.Deflater;
import java.util.zip.CRC32;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.*;

public class WriteThread extends Thread
{
    private final static int GZIP_MAGIC = 0x8b1f;
    private final static int TRAILER_SIZE = 8;

    public ByteArrayOutputStream output_stream;
    private CRC32 crc = new CRC32();
	private long total_uncmp_bytes = 0;
	
	private ArrayBlockingQueue<Block> read_compress_queue;
	private ArrayBlockingQueue<Block> compress_queue;
	private ArrayBlockingQueue<Block> write_queue;

    public WriteThread(ByteArrayOutputStream output_stream, ArrayBlockingQueue<Block> read_compress_queue, ArrayBlockingQueue<Block> write_queue)
    {
        this.output_stream = output_stream;
		this.read_compress_queue = read_compress_queue;
		this.write_queue = write_queue;
    }

    private void writeHeader() throws IOException
    {
        output_stream.write(new byte[]{
                (byte) GZIP_MAGIC,
                (byte)(GZIP_MAGIC >> 8),
                Deflater.DEFLATED,
                0,
                0,
                0,
                0,
                0,
                0,
                (byte)0x3
        });
    }

    private void writeTrailer(long totalBytes, byte[] buf, int offset) throws IOException
    {
        writeInt((int)crc.getValue(), buf, offset);
        writeInt((int)totalBytes, buf, offset +4);
    }
    private void writeInt(int i, byte[] buf, int offset) throws IOException
    {
        writeShort(i & 0xffff, buf, offset);
        writeShort((i >> 16) & 0xffff, buf, offset + 2);
    }
    private void writeShort(int s, byte[] buf, int offset) throws IOException
    {
        buf[offset] = (byte)(s & 0xff);
        buf[offset + 1] = (byte)((s >> 8) & 0xff);
    }

    public void run()
    {
        try {
            this.writeHeader();
            this.crc.reset();

            Block cur_block;
            do {				
                cur_block = write_queue.take();
				//Pigzj.PrintBlocks(write_queue, "write_queue from WRITE");
                cur_block.get_cmp_latch().await();
				//System.err.println(new String(cur_block.get_block_buf()));
				
				byte[] cur_block_buf = cur_block.get_block_buf();
				byte[] cur_cmp_block_buf = cur_block.get_cmp_block_buf();
				int cur_n_bytes_read = cur_block.get_n_bytes_read();
				
                crc.update(cur_block_buf, 0, cur_n_bytes_read);
				
				//System.err.println("Bytes read: " + cur_n_bytes_read);
				
				total_uncmp_bytes += cur_n_bytes_read;
				
				//this.output_stream.write(cur_block_buf,0, cur_n_bytes_read); //For Debug Copy Write
				
                this.output_stream.write(cur_cmp_block_buf,0, cur_block.get_n_compressed_bytes()); 
				
                read_compress_queue.put(cur_block);	

            } while(cur_block.get_is_last() == false);

            byte[] trailerBuf = new byte[TRAILER_SIZE];
            writeTrailer(total_uncmp_bytes, trailerBuf, 0);
            output_stream.write(trailerBuf);
			
            this.output_stream.writeTo(System.out);
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }
}
