import numpy as np
import matplotlib.pyplot as plt

result = np.array([
    [3.325235834469871E-7, 3.839930131314051E-7,
     1.5872102153600365E-7, 1.9330643213247937E-7],
    [4.591073920335935E-7, 3.514709614248225E-7,
     1.057230431137419E-7, 8.860240709738429E-8],
    [1.8937256475081407E-7, 1.1663707294243996E-7,
     3.0167842302219667E-8, 3.254605591934964E-8],
    [5.191884370852313E-8, 4.727071960773765E-8,
     1.7810894899410992E-8, 3.2631971093456564E-8]
])

N = [10, 20, 50, 100]

plt.plot(N, result[:, 0], label='End Point Random Start (1)')
plt.plot(N, result[:, 1], label='End Point Cyclic Start (2)')
plt.plot(N, result[:, 2], label='Complete Path Stop (4)')
plt.plot(N, result[:, 3], label='Complete Path Stop Random Start (5)')
plt.xlabel('Number of docs ratio')
plt.ylabel('Squared Error')
plt.legend()
plt.show()
