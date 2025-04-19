#include <stdio.h>
#include <stdlib.h>
#include <pthread.h>
#include <sys/time.h>

// Global variables
int N; // Matrix size
int num_threads; // Number of threads
int **A, **B, **C; // Matrices

// Thread argument structure
typedef struct {
    int thread_id;
    int start_row;
    int end_row;
} ThreadArg;

// Function prototypes
void *worker_thread(void *arg);
void read_matrix(char *filename, int **matrix);
void write_matrix(char *filename, int **matrix);
void allocate_matrix(int ***matrix, int size);
void free_matrix(int **matrix, int size);

int main(int argc, char *argv[]) {
    // Check command line arguments
    if (argc != 6) {
        printf("Usage: %s <numThreads> <N> <filename for A> <filename for B> <filename for C>\n", argv[0]);
        return 1;
    }

    // Parse command line arguments
    num_threads = atoi(argv[1]);
    N = atoi(argv[2]);
    char *file_A = argv[3];
    char *file_B = argv[4];
    char *file_C = argv[5];

    // Allocate space for matrices
    allocate_matrix(&A, N);
    allocate_matrix(&B, N);
    allocate_matrix(&C, N);

    // Read matrices A and B from files
    read_matrix(file_A, A);
    read_matrix(file_B, B);

    // Start timer
    struct timeval start, end;
    gettimeofday(&start, NULL);

    // Create worker threads
    pthread_t *threads = (pthread_t *)malloc(num_threads * sizeof(pthread_t));
    ThreadArg *thread_args = (ThreadArg *)malloc(num_threads * sizeof(ThreadArg));

    int rows_per_thread = N / num_threads;
    int extra_rows = N % num_threads;
    int current_row = 0;

    for (int i = 0; i < num_threads; i++) {
        thread_args[i].thread_id = i;
        thread_args[i].start_row = current_row;
        
        // Distribute extra rows among the first 'extra_rows' threads
        int rows_for_this_thread = rows_per_thread;
        if (i < extra_rows) {
            rows_for_this_thread++;
        }
        
        thread_args[i].end_row = current_row + rows_for_this_thread;
        current_row = thread_args[i].end_row;

        // Create thread
        if (pthread_create(&threads[i], NULL, worker_thread, (void *)&thread_args[i]) != 0) {
            perror("Failed to create thread");
            return 1;
        }
    }

    // Wait for all threads to complete
    for (int i = 0; i < num_threads; i++) {
        pthread_join(threads[i], NULL);
    }

    // Stop timer
    gettimeofday(&end, NULL);

    // Calculate execution time
    long seconds = end.tv_sec - start.tv_sec;
    long microseconds = end.tv_usec - start.tv_usec;
    double execution_time = seconds + microseconds / 1000000.0;

    // Print execution time
    printf("Execution time: %.6f seconds\n", execution_time);

    // Write result matrix C to output file
    write_matrix(file_C, C);

    // Free memory
    free_matrix(A, N);
    free_matrix(B, N);
    free_matrix(C, N);
    free(threads);
    free(thread_args);

    return 0;
}

// Worker thread function
void *worker_thread(void *arg) {
    ThreadArg *thread_arg = (ThreadArg *)arg;
    int start_row = thread_arg->start_row;
    int end_row = thread_arg->end_row;

    // Perform matrix multiplication for assigned rows
    for (int i = start_row; i < end_row; i++) {
        for (int j = 0; j < N; j++) {
            C[i][j] = 0;
            for (int k = 0; k < N; k++) {
                C[i][j] += A[i][k] * B[k][j];
            }
        }
    }

    return NULL;
}

// Function to read matrix from file
void read_matrix(char *filename, int **matrix) {
    FILE *fp = fopen(filename, "r");
    if (fp == NULL) {
        printf("Error opening file %s\n", filename);
        exit(1);
    }

    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            if (fscanf(fp, "%d", &matrix[i][j]) != 1) {
                printf("Error reading from file %s\n", filename);
                exit(1);
            }
        }
    }

    fclose(fp);
}

// Function to write matrix to file
void write_matrix(char *filename, int **matrix) {
    FILE *fp = fopen(filename, "w");
    if (fp == NULL) {
        printf("Error opening file %s\n", filename);
        exit(1);
    }

    for (int i = 0; i < N; i++) {
        for (int j = 0; j < N; j++) {
            fprintf(fp, "%d\n", matrix[i][j]);
        }
    }

    fclose(fp);
}

// Function to allocate memory for a matrix
void allocate_matrix(int ***matrix, int size) {
    *matrix = (int **)malloc(size * sizeof(int *));
    if (*matrix == NULL) {
        printf("Memory allocation failed\n");
        exit(1);
    }

    for (int i = 0; i < size; i++) {
        (*matrix)[i] = (int *)malloc(size * sizeof(int));
        if ((*matrix)[i] == NULL) {
            printf("Memory allocation failed\n");
            exit(1);
        }
    }
}

// Function to free memory allocated for a matrix
void free_matrix(int **matrix, int size) {
    for (int i = 0; i < size; i++) {
        free(matrix[i]);
    }
    free(matrix);
}