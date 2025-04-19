#include <stdio.h>
#include <stdlib.h>
#include <time.h>

int main(int argc, char *argv[]){


    if (argc < 3){
        printf("Usage: %s <matrix size (n)> <outputFile>\n", argv[0]);
        return 0;
    } //end-if

    int n = atoi(argv[1]);
    FILE *fp = fopen(argv[2], "w");
    if (fp == NULL){
      printf("Can't create file %s\n", argv[2]);
      exit(1);
    } // end-if

    // Initialize random number seed
    srand48(time(0));
    for (int i=0; i < n*n; i++){
        int num = (int)(drand48()*11)-5;
        fprintf(fp, "%d\n", num);
    } //end-for
    fclose(fp);

    return 0;
} //end-main
