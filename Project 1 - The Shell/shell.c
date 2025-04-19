#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/wait.h>
#include <time.h>

#define MAX_LINE 1024
#define MAX_ARGS 100
#define MAX_CMDS 10


void parse_args(char *cmd, char **args) {
    int i = 0;
    char *token = strtok(cmd, " \t");  // Parse on both spaces and tabs
    while (token != NULL && i < MAX_ARGS - 1) {
        args[i++] = token;
        token = strtok(NULL, " \t");
    }
    args[i] = NULL;
}
// Process I/O redirection in the command arguments
void process_io_redirection(char **args, int *in_redirect, int *out_redirect) {
    int i = 0;
    while (args[i] != NULL) {
        if (strcmp(args[i], "<") == 0 && args[i+1] != NULL) {
            *in_redirect = open(args[i+1], O_RDONLY);
            if (*in_redirect < 0) {
                perror("open < failed");
                exit(1);
            }
            // Remove the redirection symbols and filename from args
            args[i] = NULL;
            i += 2;
        } else if (strcmp(args[i], ">") == 0 && args[i+1] != NULL) {
            *out_redirect = open(args[i+1], O_WRONLY | O_CREAT | O_TRUNC, 0644);
            if (*out_redirect < 0) {
                perror("open > failed");
                exit(1);
            }
            // Remove the redirection symbols and filename from args
            args[i] = NULL;
            i += 2;
        } else {
            i++;
        }
    }
}

// Execute a single command with its args
void execute_command(char *cmd, int input_fd, int output_fd) {
    char *args[MAX_ARGS];
    int in_redirect = -1, out_redirect = -1;
    char *cmd_copy = strdup(cmd);
    parse_args(cmd_copy, args);
    process_io_redirection(args, &in_redirect, &out_redirect);
    // Set up input redirection
    if (in_redirect != -1) {
        dup2(in_redirect, STDIN_FILENO);
        close(in_redirect);
    } else if (input_fd != STDIN_FILENO) {
        dup2(input_fd, STDIN_FILENO);
        close(input_fd);
    }
    // Set up output redirection
    if (out_redirect != -1) {
        dup2(out_redirect, STDOUT_FILENO);
        close(out_redirect);
    } else if (output_fd != STDOUT_FILENO) {
        dup2(output_fd, STDOUT_FILENO);
        close(output_fd);
    }
    // Execute the command
    if (args[0] != NULL) {
    execvp(args[0], args);
    perror("execvp failed");
}
    free(cmd_copy);
    exit(1);
}

int main() {
    char line[MAX_LINE];
    while (1) {
        printf("CS340Shell%% ");
        fflush(stdout);
        
        if (fgets(line, sizeof(line), stdin) == NULL) break;
        // Remove trailing newline
        line[strcspn(line, "\n")] = '\0'; 
        // Skip empty lines
        if (strlen(line) == 0) continue;
        // Handle internal commands
        if (strcmp(line, "exit") == 0) {
            break;
        } else if (strcmp(line, "time") == 0) {
            time_t now = time(NULL);
            printf("%s", ctime(&now));
            continue;
        } else if (strncmp(line, "cd ", 3) == 0) {
            char *dir = line + 3;
            if (chdir(dir) != 0) {
                perror("cd failed");
            }
            continue;
        }
        // Split the line into separate commands by pipe symbol
        char *commands[MAX_CMDS];
        int num_cmds = 0;
        // Create a copy of the line because strtok modifies the string
        char *line_copy = strdup(line);
        char *cmd = strtok(line_copy, "|");
        
        while (cmd != NULL && num_cmds < MAX_CMDS) {
            // Trim leading spaces
            while (*cmd == ' ' || *cmd == '\t') cmd++;
            commands[num_cmds++] = strdup(cmd);
            cmd = strtok(NULL, "|");
        }
        free(line_copy);
        // Set up pipes and execute commands
        int pipes[MAX_CMDS-1][2];
        pid_t pids[MAX_CMDS];
        
        // Create all necessary pipes
        for (int i = 0; i < num_cmds - 1; i++) {
            if (pipe(pipes[i]) < 0) {
                perror("pipe creation failed");
                exit(1);
            }
        }
        // Fork and execute all commands
        for (int i = 0; i < num_cmds; i++) {
            pids[i] = fork();
            if (pids[i] < 0) {
                perror("fork failed");
                exit(1);
            } else if (pids[i] == 0) {
                // Child process- Set up input (from previous pipe or stdin)
                int input_fd = STDIN_FILENO;
                if (i > 0) {
                    input_fd = pipes[i-1][0];
                }
                // Set up output (to next pipe or stdout)
                int output_fd = STDOUT_FILENO;
                if (i < num_cmds - 1) {
                    output_fd = pipes[i][1];
                }
                // Close all unused pipe ends
                for (int j = 0; j < num_cmds - 1; j++) {
                    if (j != i - 1) close(pipes[j][0]);
                    if (j != i) close(pipes[j][1]);
                }
                execute_command(commands[i], input_fd, output_fd);
                // Should not reach here
                exit(1);
            }
        }
        // Parent closes all pipe ends
        for (int i = 0; i < num_cmds - 1; i++) {
            close(pipes[i][0]);
            close(pipes[i][1]);
        }
        // Wait for all children to finish
        for (int i = 0; i < num_cmds; i++) {
            waitpid(pids[i], NULL, 0);
        }
        // Free allocated memory
        for (int i = 0; i < num_cmds; i++) {
            free(commands[i]);
        }
    }
    return 0;
}