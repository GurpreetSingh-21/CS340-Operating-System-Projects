import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Scanner;

public class schedule {
    static class Process {
        int id;
        int arrivalTime;
        int burstTime;
        int priority;
        int remainingTime;
        int finishTime;
        
        public Process(int id, int arrivalTime, int burstTime, int priority) {
            this.id = id;
            this.arrivalTime = arrivalTime;
            this.burstTime = burstTime;
            this.priority = priority;
            this.remainingTime = burstTime;
        }
        
        public int getTurnaroundTime() {
            return finishTime - arrivalTime;
        }
        
        @Override
        public String toString() {
            return id + "," + arrivalTime + "," + finishTime + "," + getTurnaroundTime();
        }
    }
    
    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: schedule <test file name> <scheduling algorithm> <time quantum if rr is being used>");
            return;
        }
        
        String testFile = args[0];
        String algorithm = args[1];
        int timeQuantum = 0;
        
        if (algorithm.equals("rr")) {
            if (args.length < 3) {
                System.out.println("Time quantum is required for Round Robin scheduling");
                return;
            }
            timeQuantum = Integer.parseInt(args[2]);
        }
        
        List<Process> processes = readProcesses(testFile);
        if (processes == null) return;
        
        List<Process> result = null;
        
        switch (algorithm) {
            case "fcfs":
                result = fcfs(processes);
                break;
            case "srtf":
                result = srtf(processes);
                break;
            case "pri":
                result = priority(processes);
                break;
            case "rr":
                result = roundRobin(processes, timeQuantum);
                break;
            default:
                System.out.println("Unknown scheduling algorithm: " + algorithm);
                return;
        }
        
        // Print results
        for (Process p : result) {
            System.out.println(p);
        }
    }
    
    private static List<Process> readProcesses(String filename) {
        List<Process> processes = new ArrayList<>();
        
        try {
            Scanner scanner = new Scanner(new File(filename));
            
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                String[] parts = line.split(",");
                
                if (parts.length >= 4) {
                    int id = Integer.parseInt(parts[0]);
                    int arrivalTime = Integer.parseInt(parts[1]);
                    int burstTime = Integer.parseInt(parts[2]);
                    int priority = Integer.parseInt(parts[3]);
                    
                    processes.add(new Process(id, arrivalTime, burstTime, priority));
                }
            }
            
            scanner.close();
            
        } catch (FileNotFoundException e) {
            System.out.println("File not found: " + filename);
            return null;
        }
        
        return processes;
    }
    
    // First Come First Served (Non-Preemptive)
    private static List<Process> fcfs(List<Process> originalProcesses) {
        // Create deep copy of processes
        List<Process> processes = deepCopy(originalProcesses);
        
        int currentTime = 0;
        int processIndex = 0;
        
        while (processIndex < processes.size()) {
            Process currentProcess = processes.get(processIndex);
            
            // If process hasn't arrived yet, move time forward
            if (currentTime < currentProcess.arrivalTime) {
                currentTime = currentProcess.arrivalTime;
            }
            
            // Execute process
            currentTime += currentProcess.burstTime;
            currentProcess.finishTime = currentTime;
            
            processIndex++;
        }
        
        return processes;
    }
    
    // Shortest Remaining Time First (Preemptive)
    private static List<Process> srtf(List<Process> originalProcesses) {
        // Create deep copy of processes
        List<Process> processes = deepCopy(originalProcesses);
        
        int n = processes.size();
        int completed = 0;
        int currentTime = 0;
        
        // Sort processes by remaining time and process ID for tie-breaking
        Comparator<Process> srtfComparator = (p1, p2) -> {
            if (p1.remainingTime != p2.remainingTime)
                return p1.remainingTime - p2.remainingTime;
            return p1.id - p2.id;
        };
        
        PriorityQueue<Process> readyQueue = new PriorityQueue<>(srtfComparator);
        Process currentProcess = null;
        
        while (completed < n) {
            // Add newly arrived processes to ready queue
            for (Process p : processes) {
                if (p.arrivalTime == currentTime && p.remainingTime > 0) {
                    readyQueue.add(p);
                }
            }
            
            // If there's a running process, check if it should be preempted
            if (currentProcess != null && currentProcess.remainingTime > 0) {
                // If the ready queue has a process with shorter remaining time, preempt
                if (!readyQueue.isEmpty() && readyQueue.peek().remainingTime < currentProcess.remainingTime) {
                    readyQueue.add(currentProcess);
                    currentProcess = readyQueue.poll();
                }
            } else if (currentProcess == null || currentProcess.remainingTime == 0) {
                // If no process is running or current process has finished, get the next one
                if (!readyQueue.isEmpty()) {
                    if (currentProcess != null && currentProcess.remainingTime == 0) {
                        currentProcess.finishTime = currentTime;
                        completed++;
                    }
                    currentProcess = readyQueue.poll();
                } else if (currentProcess != null && currentProcess.remainingTime == 0) {
                    currentProcess.finishTime = currentTime;
                    completed++;
                    currentProcess = null;
                }
            }
            
            // Execute the current process for one time unit
            if (currentProcess != null && currentProcess.remainingTime > 0) {
                currentProcess.remainingTime--;
                if (currentProcess.remainingTime == 0) {
                    currentProcess.finishTime = currentTime + 1;
                    completed++;
                    currentProcess = null;
                }
            }
            
            currentTime++;
            
            // If all processes have arrived but aren't in ready queue, add them
            if (readyQueue.isEmpty() && currentProcess == null && completed < n) {
                // Find next process to arrive
                int nextArrivalTime = Integer.MAX_VALUE;
                Process nextProcess = null;
                
                for (Process p : processes) {
                    if (p.remainingTime > 0 && p.arrivalTime < nextArrivalTime && p.arrivalTime > currentTime) {
                        nextArrivalTime = p.arrivalTime;
                        nextProcess = p;
                    }
                }
                
                if (nextProcess != null) {
                    currentTime = nextArrivalTime;
                }
            }
        }
        
        // Sort by process ID for output
        processes.sort(Comparator.comparingInt(p -> p.id));
        return processes;
    }
    
    // Priority Scheduling (Preemptive)
    private static List<Process> priority(List<Process> originalProcesses) {
        // Create deep copy of processes
        List<Process> processes = deepCopy(originalProcesses);
        
        int n = processes.size();
        int completed = 0;
        int currentTime = 0;
        
        // Sort processes by priority (lower value = higher priority) and process ID for tie-breaking
        Comparator<Process> priorityComparator = (p1, p2) -> {
            if (p1.priority != p2.priority)
                return p1.priority - p2.priority;
            return p1.id - p2.id;
        };
        
        PriorityQueue<Process> readyQueue = new PriorityQueue<>(priorityComparator);
        Process currentProcess = null;
        
        while (completed < n) {
            // Add newly arrived processes to ready queue
            for (Process p : processes) {
                if (p.arrivalTime == currentTime && p.remainingTime > 0) {
                    readyQueue.add(p);
                }
            }
            
            // If there's a running process, check if it should be preempted
            if (currentProcess != null && currentProcess.remainingTime > 0) {
                // If the ready queue has a higher priority process, preempt
                if (!readyQueue.isEmpty() && 
                    (readyQueue.peek().priority < currentProcess.priority || 
                     (readyQueue.peek().priority == currentProcess.priority && 
                      readyQueue.peek().id < currentProcess.id))) {
                    readyQueue.add(currentProcess);
                    currentProcess = readyQueue.poll();
                }
            } else if (currentProcess == null || currentProcess.remainingTime == 0) {
                // If no process is running or current process has finished, get the next one
                if (!readyQueue.isEmpty()) {
                    if (currentProcess != null && currentProcess.remainingTime == 0) {
                        currentProcess.finishTime = currentTime;
                        completed++;
                    }
                    currentProcess = readyQueue.poll();
                } else if (currentProcess != null && currentProcess.remainingTime == 0) {
                    currentProcess.finishTime = currentTime;
                    completed++;
                    currentProcess = null;
                }
            }
            
            // Execute the current process for one time unit
            if (currentProcess != null && currentProcess.remainingTime > 0) {
                currentProcess.remainingTime--;
                if (currentProcess.remainingTime == 0) {
                    currentProcess.finishTime = currentTime + 1;
                    completed++;
                    currentProcess = null;
                }
            }
            
            currentTime++;
            
            // If all processes have arrived but aren't in ready queue, add them
            if (readyQueue.isEmpty() && currentProcess == null && completed < n) {
                // Find next process to arrive
                int nextArrivalTime = Integer.MAX_VALUE;
                Process nextProcess = null;
                
                for (Process p : processes) {
                    if (p.remainingTime > 0 && p.arrivalTime < nextArrivalTime && p.arrivalTime > currentTime) {
                        nextArrivalTime = p.arrivalTime;
                        nextProcess = p;
                    }
                }
                
                if (nextProcess != null) {
                    currentTime = nextArrivalTime;
                }
            }
        }
        
        // Sort by process ID for output
        processes.sort(Comparator.comparingInt(p -> p.id));
        return processes;
    }
    
    // Round Robin (Preemptive with Time Quantum)
    private static List<Process> roundRobin(List<Process> originalProcesses, int timeQuantum) {
        // Create deep copy of processes
        List<Process> processes = deepCopy(originalProcesses);
        
        int n = processes.size();
        int completed = 0;
        int currentTime = 0;
        
        List<Process> readyQueue = new ArrayList<>();
        int processIndex = 0;
        
        while (completed < n) {
            // Add newly arrived processes to ready queue
            while (processIndex < n && processes.get(processIndex).arrivalTime <= currentTime) {
                readyQueue.add(processes.get(processIndex));
                processIndex++;
            }
            
            // If ready queue is empty but there are more processes to arrive
            if (readyQueue.isEmpty() && processIndex < n) {
                currentTime = processes.get(processIndex).arrivalTime;
                continue;
            }
            
            // If all processes are done, break
            if (readyQueue.isEmpty()) {
                break;
            }
            
            // Get the next process from the ready queue
            Process currentProcess = readyQueue.remove(0);
            
            // Calculate execution time for this time slice
            int executeTime = Math.min(timeQuantum, currentProcess.remainingTime);
            currentTime += executeTime;
            currentProcess.remainingTime -= executeTime;
            
            // Check for any new arrivals during this time slice
            while (processIndex < n && processes.get(processIndex).arrivalTime <= currentTime) {
                readyQueue.add(processes.get(processIndex));
                processIndex++;
            }
            
            // If process is done, mark its finish time
            if (currentProcess.remainingTime == 0) {
                currentProcess.finishTime = currentTime;
                completed++;
            } else {
                // Otherwise, put it back at the end of the queue
                readyQueue.add(currentProcess);
            }
        }
        
        // Sort by process ID for output
        processes.sort(Comparator.comparingInt(p -> p.id));
        return processes;
    }
    
    // Helper method to create a deep copy of the processes list
    private static List<Process> deepCopy(List<Process> original) {
        List<Process> copy = new ArrayList<>();
        for (Process p : original) {
            copy.add(new Process(p.id, p.arrivalTime, p.burstTime, p.priority));
        }
        return copy;
    }
}