"""Independent checks for the selected Java solution. Run from repository root."""
import json
import random
import subprocess
import sys
from functools import lru_cache

RNG = random.Random(20261108)
RUN = ['java', '-Xmx384m', '-cp', 'Java/build', 'Main']


def ask(inputs):
    payload = ''.join(json.dumps(x, separators=(',', ':')) + '\n' for x in inputs)
    process = subprocess.run(RUN, input=payload, text=True, capture_output=True,
                             encoding='utf-8', timeout=20, check=True)
    lines = process.stdout.splitlines()
    assert len(lines) == len(inputs), (len(lines), len(inputs), process.stderr)
    return [json.loads(x) for x in lines]


def subseq(s, t):
    pos = 0
    for c in s:
        pos = t.find(c, pos)
        if pos < 0:
            return False
        pos += 1
    return True


def lcs_length(a, b):
    row = [0] * (len(b) + 1)
    for c in a:
        prev = 0
        for j, d in enumerate(b, 1):
            old = row[j]
            row[j] = prev + 1 if c == d else max(row[j], row[j - 1])
            prev = old
    return row[-1]


def merge_cost(sizes):
    p = [0]
    for x in sizes:
        p.append(p[-1] + x)
    @lru_cache(None)
    def f(l, r):
        if l == r:
            return 0
        return min(f(l, k) + f(k + 1, r) + p[r+1] - p[l] for k in range(l, r))
    return f(0, len(sizes) - 1) if sizes else 0


def check_merge(sizes, output):
    blocks = [(i, i) for i in range(len(sizes))]
    total = 0
    for l, k, r in output['plan']:
        assert (l, k) in blocks
        index = blocks.index((l, k))
        assert index + 1 < len(blocks) and blocks[index+1] == (k + 1, r)
        blocks[index:index + 2] = [(l, r)]
        total += sum(sizes[l:r+1])
    assert len(output['plan']) == max(0, len(sizes) - 1)
    assert len(blocks) == (1 if sizes else 0)
    assert output['cost'] == total == merge_cost(sizes)


def tree_brute(weights, edges):
    n = len(weights)
    return max((sum(weights[i] for i in range(n) if mask >> i & 1)
                for mask in range(1 << n)
                if all(not (mask >> u & 1 and mask >> v & 1) for u, v in edges)), default=0)


def check_tree(weights, edges, output, expected=None):
    chosen = output['selected']
    assert len(chosen) == len(set(chosen))
    assert all(0 <= i < len(weights) for i in chosen)
    chosen_set = set(chosen)
    assert all(u not in chosen_set or v not in chosen_set for u, v in edges)
    assert output['weight'] == sum(weights[i] for i in chosen)
    if expected is not None:
        assert output['weight'] == expected


def check_suffix(text, output):
    n = len(text)
    sa, lcp = output['sa'], output['lcp']
    assert sa == sorted(range(n), key=lambda i: text[i:])
    assert len(lcp) == n
    for i in range(n):
        common = 0
        if i:
            a, b = sa[i-1], sa[i]
            while a+common < n and b+common < n and text[a+common] == text[b+common]:
                common += 1
        assert lcp[i] == common
    repeated = output['repeated']
    assert len(repeated) == max(lcp, default=0)
    assert not repeated or sum(text.startswith(repeated, i) for i in range(n)) >= 2


def match_positions(text, pattern):
    if not pattern:
        return list(range(len(text) + 1))
    return [i for i in range(len(text) - len(pattern) + 1)
            if text[i:i + len(pattern)] == pattern]


def aho_expected(patterns, texts):
    return [[sum(text[i:i+len(p)] == p for i in range(len(text) - len(p) + 1))
             for p in patterns] for text in texts]


def random_tests():
    requests = []
    for _ in range(150):
        a = ''.join(RNG.choices('aABC_', k=RNG.randrange(10)))
        b = ''.join(RNG.choices('aABC_', k=RNG.randrange(10)))
        requests.append({'task':'lcs', 'a':a, 'b':b})
        sizes = [RNG.randrange(10) for _ in range(RNG.randrange(8))]
        requests.append({'task':'merge', 'sizes':sizes})
        n = RNG.randrange(11)
        weights = [RNG.randrange(-12, 13) for _ in range(n)]
        edges = [[i, RNG.randrange(i)] for i in range(1, n)]
        requests.append({'task':'tree','n':n,'weights':weights,'edges':edges})
        text = ''.join(RNG.choices('abcd! ', k=RNG.randrange(22)))
        pattern = ''.join(RNG.choices('abcd! ', k=RNG.randrange(10)))
        for task in ('naive','kmp','rk'):
            requests.append({'task':task,'text':text,'pattern':pattern})
        requests.append({'task':'suffix','text': ''.join(RNG.choices('abc', k=RNG.randrange(20)))})
        patterns = [''.join(RNG.choices('ab ', k=RNG.randrange(1, 6))) for _ in range(RNG.randrange(8))]
        texts = [''.join(RNG.choices('ab ', k=RNG.randrange(18))) for _ in range(RNG.randrange(5))]
        requests.append({'task':'aho','patterns':patterns,'texts':texts})
    responses = ask(requests + requests)
    assert responses[:len(requests)] == responses[len(requests):], 'Persistent-state leak'
    for request, output in zip(requests, responses):
        task = request['task']
        if task == 'lcs':
            assert output['length'] == lcs_length(request['a'], request['b'])
            assert len(output['sequence']) == output['length']
            assert subseq(output['sequence'],request['a']) and subseq(output['sequence'],request['b'])
        elif task == 'merge':
            check_merge(request['sizes'], output)
        elif task == 'tree':
            check_tree(request['weights'], request['edges'], output,
                       tree_brute(request['weights'], request['edges']))
        elif task in ('naive','kmp','rk'):
            assert output['positions'] == match_positions(request['text'], request['pattern'])
        elif task == 'suffix':
            check_suffix(request['text'], output)
        elif task == 'aho':
            assert output['counts'] == aho_expected(request['patterns'], request['texts'])
    print('PASS: 1200 independently checked randomized requests, plus repeated batch')


def boundary_tests():
    cases = [
        ({'task':'lcs','a':'','b':'abc'}, {'length':0, 'sequence':''}),
        ({'task':'merge','sizes':[]}, {'cost':0, 'plan':[]}),
        ({'task':'merge','sizes':[10**9,10**9,10**9]}, None),
        ({'task':'tree','n':0,'weights':[],'edges':[]}, {'weight':0,'selected':[]}),
        ({'task':'tree','n':3,'weights':[-4,-3,-5],'edges':[[0,1],[1,2]]}, {'weight':0,'selected':[]}),
        ({'task':'naive','text':'ab','pattern':''}, {'positions':[0,1,2]}),
        ({'task':'kmp','text':'abababa','pattern':'aba'}, {'positions':[0,2,4]}),
        ({'task':'rk','text':'abdj','pattern':'ab'}, {'positions':[0]}),
        ({'task':'rk','text':'bcek','pattern':'bc'}, {'positions':[0]}),
        ({'task':'suffix','text':'banana'}, {'sa':[5,3,1,0,4,2],'lcp':[0,1,3,0,0,2],'repeated':'ana'}),
        ({'task':'suffix','text':''}, {'sa':[],'lcp':[],'repeated':''}),
        ({'task':'aho','patterns':[],'texts':['a','']}, {'counts':[[],[]]}),
        ({'task':'aho','patterns':['a','aa','a'],'texts':['aaa','','aaa']},
         {'counts':[[3,2,3],[0,0,0],[3,2,3]]}),
    ]
    answers = ask([x for x, _ in cases])
    for (x, expected), answer in zip(cases, answers):
        if expected is None:
            check_merge(x['sizes'], answer)
        else:
            assert answer == expected, (x, answer, expected)
    print('PASS: 13 explicit boundary and collision cases')


def scale_tests():
    n = 100000
    path = [[i, i+1] for i in range(n-1)]
    weights = [10**9 if i % 2 == 0 else 0 for i in range(n)]
    result = ask([{'task':'tree','n':n,'weights':weights,'edges':path}])[0]
    check_tree(weights,path,result,50_000_000_000_000)
    text = 'a'*50000
    result = ask([{'task':'suffix','text':text}])[0]
    assert result['sa'] == list(range(49999,-1,-1))
    assert result['lcp'] == list(range(50000))
    assert len(result['repeated']) == 49999
    large = 'ab' * 100000
    result = ask([{'task':'kmp','text':large,'pattern':'aba'}])[0]
    assert result['positions'] == list(range(0, 199998, 2))
    patterns = ['a'*i for i in range(1, 301)] + ['a'] * 1000
    result = ask([{'task':'aho','patterns':patterns,'texts':['a' * 10000,'a'*10000]}])[0]
    assert result['counts'][0] == result['counts'][1]
    assert result['counts'][0][:300] == [10001-i for i in range(1,301)]
    assert result['counts'][0][300:] == [10000]*1000
    print('PASS: 4 scale/stress cases (100k tree, 50k suffix, 200k KMP, dense Aho)')

if __name__ == '__main__':
    boundary_tests()
    random_tests()
    if '--stress' in sys.argv:
        scale_tests()
