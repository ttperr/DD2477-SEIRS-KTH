import numpy as np
data = np.loadtxt('Assignment 2/task 2.5.txt', comments='//',
                  delimiter=' ', usecols=(0, 2), dtype=int)
print(data)

run_numbers = data[:, 0]
true_label = data[:, 1]

unique_run_number = np.unique(run_numbers)

relevant_docs = np.zeros(len(unique_run_number))
retrieved_docs = np.zeros(len(unique_run_number))
relevant_retrieved_docs = np.zeros(len(unique_run_number))

for i in unique_run_number:
    for x in data:
        if x[0] == i:
            retrieved_docs[i-1] += 1
            if x[1] > 0:
                relevant_docs[i-1] += 1

for i in range(len(unique_run_number)):
    precision = relevant_docs[i] / retrieved_docs[i]
    recall = relevant_docs[i] / 100
    print(
        f'Run {i+1}: Precision: {precision%1:.3f}, Recall: {recall%1:.3f}')
