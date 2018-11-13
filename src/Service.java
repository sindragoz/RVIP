import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Service {

		int THREAD_NUMBER = 0;		
		long startTime;
		long finishTime;
		int[][] arr;
		int min=Integer.MAX_VALUE;
		Random r;
		ReentrantLock locker;
		Condition cond;
		ThreadPoolExecutor executor;
		
		public Service(int threadNumber, int matrixSize, Random r) {
			THREAD_NUMBER = threadNumber;
			this.r=r;
			arr=generateRandomArray(matrixSize);
			
		}
		
		private int[][] generateRandomArray(int n){
			Random r=new Random();
			int arr[][]=new int[n][n];
			for(int i=0;i<n;i++) {
				for(int j=0;j<n;j++){
					arr[i][j] = r.nextInt(100);
					System.out.print(arr[i][j]+" - ");//rand.nextInt(4)+1;
				}
			}
			return arr;
		}
		
		public void execute() {
			executor = (ThreadPoolExecutor)Executors.newFixedThreadPool(THREAD_NUMBER);
			locker = new ReentrantLock();
			cond = locker.newCondition();
			startTime = System.nanoTime();

			for (int i = 0; i < arr[0].length; i++) {
				
				try {
					locker.lock();
					while (executor.getQueue().size() > executor.getMaximumPoolSize()) {
						cond.await();
						System.out.println("Waiting queue - " +executor.getQueue().size());
					} 
							executor.submit(new RowTask(arr[i],i));
				} catch (Exception e) {
					System.out.println(e.getMessage());
					System.out.println("Interrupt error");
				} finally {
					cond.signal();
					locker.unlock();
				}
			}
			try {
				locker.lock();
				while (executor.getActiveCount() > 0) {
					
					cond.await();
					System.out.println("Waiting active - "+executor.getActiveCount());
				} 
				
			} catch (InterruptedException e) {
				e.printStackTrace();
				System.out.println("Interrupt error");
			}finally {
				cond.signal();
				locker.unlock();
			}
			
			executor.shutdown();			
			finishTime = System.nanoTime();
			System.out.println("Выполнение завершено!");
			System.out.println("Минимальный элемент матрицы: " + min);
			System.out.println("Затраченное время: " + (finishTime - startTime) + " наносекунд ("+(finishTime - startTime)/(int)1e6+" милисекунд)");

		}
		class RowTask implements Runnable {
			private int[] row;
			int i;
			public RowTask(int[] row, int i) {
				this.row = row;
				this.i=i;
			}
			private void signalToContinue() {
				locker.lock();
				cond.signal();
				locker.unlock();
			}
			@Override
			public void run() {
				try {
					Thread.sleep(200);					
					System.out.println(this.i+"-й поток выполняется...");
					for (int i = 0; i < row.length; i++) {
						min=Math.min(min, row[i]);
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
					System.out.println("Interrupt error");
				} finally {
					signalToContinue();
					System.out.println(this.i+"-й поток завершился.");
				}
			}
		}
	}