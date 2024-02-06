import numpy as np
import matplotlib.pyplot as plt

data = np.loadtxt('Assignment 2/task 2.5.txt', comments='//',
                  delimiter=' ', usecols=(0, 2), dtype=int)

run_numbers = data[:, 0]
true_label = data[:, 1]

unique_run_number = np.unique(run_numbers)

relevant_docs = np.zeros(len(unique_run_number))
retrieved_docs = np.zeros(len(unique_run_number))
relevant_retrieved_docs = np.zeros(len(unique_run_number))

precision_by_tenth = []
recall_by_tenth = []

for i in unique_run_number:
    precision_by_tenth.append([])
    recall_by_tenth.append([])
    k = 0
    for x in data:
        if x[0] == i:
            k += 1
            retrieved_docs[i-1] += 1
            if x[1] > 0:
                relevant_docs[i-1] += 1
            if k % 10 == 0:
                precision_by_tenth[i-1].append(
                    relevant_docs[i-1] / retrieved_docs[i-1])
                recall_by_tenth[i-1].append(relevant_docs[i-1] / 100)

for i in range(len(unique_run_number)):
    precision = relevant_docs[i] / retrieved_docs[i]
    recall = relevant_docs[i] / 100
    print(
        f'Run {i+1}: Precision: {precision%1:.3f}, Recall: {recall%1:.3f}')

fig, axs = plt.subplots(1, 2)
for i in range(len(unique_run_number)):
    axs[0].plot(precision_by_tenth[i])
    axs[1].plot(recall_by_tenth[i])

axs[0].set_title('Precision by 10th')
axs[1].set_title('Recall by 10th')
axs[0].set_xlabel('10th')
axs[1].set_xlabel('10th')
axs[0].set_ylabel('Precision')
axs[1].set_ylabel('Recall')
fig.suptitle('Precision and Recall by 10th')
plt.tight_layout()
axs[0].legend([f'Run {i+1}' for i in range(len(unique_run_number))])
axs[1].legend([f'Run {i+1}' for i in range(len(unique_run_number))])
plt.show()
