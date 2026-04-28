import ReactMarkdown, { type Components } from 'react-markdown';
import remarkGfm from 'remark-gfm';

function markdownProps<T extends { node?: unknown }>(props: T) {
  const { node, ...rest } = props;
  void node;
  return rest;
}

const components: Components = {
  h1: (props) => (
    <h2 {...markdownProps(props)} style={{ fontSize: 18, fontWeight: 600, margin: '18px 0 10px', color: '#262626' }} />
  ),
  h2: (props) => (
    <h3 {...markdownProps(props)} style={{ fontSize: 16, fontWeight: 600, margin: '16px 0 8px', color: '#262626' }} />
  ),
  h3: (props) => (
    <h4 {...markdownProps(props)} style={{ fontSize: 15, fontWeight: 600, margin: '14px 0 6px', color: '#722ed1' }} />
  ),
  p: (props) => (
    <p {...markdownProps(props)} style={{ margin: '8px 0', color: '#595959', lineHeight: 1.8 }} />
  ),
  ul: (props) => (
    <ul {...markdownProps(props)} style={{ paddingLeft: 22, margin: '8px 0', listStyleType: 'disc' }} />
  ),
  ol: (props) => (
    <ol {...markdownProps(props)} style={{ paddingLeft: 22, margin: '8px 0', listStyleType: 'decimal' }} />
  ),
  li: (props) => (
    <li {...markdownProps(props)} style={{ marginBottom: 4, color: '#595959', lineHeight: 1.7 }} />
  ),
  strong: (props) => (
    <strong {...markdownProps(props)} style={{ color: '#262626' }} />
  ),
  pre: (props) => (
    <pre {...markdownProps(props)} style={{ background: '#f5f5f5', padding: 12, borderRadius: 6, overflow: 'auto', fontSize: 12, margin: '10px 0' }} />
  ),
  code: ({ className, ...props }) => (
    <code {...markdownProps(props)} className={className} style={{ background: '#f5f5f5', padding: '1px 4px', borderRadius: 3, fontSize: 12, color: '#d4380d' }} />
  ),
  table: (props) => (
    <div style={{ overflowX: 'auto', margin: '12px 0' }}>
      <table {...markdownProps(props)} style={{ width: '100%', borderCollapse: 'collapse', fontSize: 13 }} />
    </div>
  ),
  th: (props) => (
    <th {...markdownProps(props)} style={{ border: '1px solid #d9d9d9', background: '#fafafa', padding: '8px 10px', textAlign: 'left', fontWeight: 600 }} />
  ),
  td: (props) => (
    <td {...markdownProps(props)} style={{ border: '1px solid #d9d9d9', padding: '8px 10px', verticalAlign: 'top' }} />
  ),
  blockquote: (props) => (
    <blockquote {...markdownProps(props)} style={{ borderLeft: '4px solid #d9d9d9', paddingLeft: 12, margin: '10px 0', color: '#595959' }} />
  ),
  hr: () => <hr style={{ border: 'none', borderTop: '1px solid #f0f0f0', margin: '14px 0' }} />,
};

interface MarkdownContentProps {
  content: string;
}

export default function MarkdownContent({ content }: MarkdownContentProps) {
  return (
    <ReactMarkdown remarkPlugins={[remarkGfm]} components={components}>
      {content}
    </ReactMarkdown>
  );
}
