#!/bin/bash

# Check if a file name is provided
if [ "$#" -ne 1 ]; then
    echo "Usage: $0 <filename>"
    exit 1
fi

# File to process
file=$1

# Using awk to process the file
awk '
BEGIN {
    FS=" "
}

{
    # If line starts with //, skip it
    if ($1 == "//") {
        next
    }

    query_id=$1
    relevance_score=$3

    # Count total retrieved for each query_id
    count_retrieved[query_id]++

    # Count relevant (relevance_score >= 1) for each query_id
    if (relevance_score >= 1) {
        count_relevant_retrieved[query_id]++
    }
}

END {
    for (id in count_retrieved) {
        # Calculate precision and recall
        precision = count_relevant_retrieved[id] / count_retrieved[id]
        recall = count_relevant_retrieved[id] / 100

        # Print results
        printf "Query ID: %d, Precision: %.2f, Recall: %.2f\n", id, precision, recall
    }
}' "$file"